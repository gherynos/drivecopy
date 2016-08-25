/**
 * Copyright 2012-2015 Luca Zanconato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nharyes.drivecopy;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.nharyes.drivecopy.biz.bo.FileBO;
import net.nharyes.drivecopy.biz.wfm.FileStorageWorkflowManager;
import net.nharyes.drivecopy.log.SystemOutHandler;
import net.nharyes.drivecopy.mod.MainModule;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {

	/*
	 * Version
	 */
	public static final String VERSION = "1.2.2";

	/*
	 * Logger
	 */
	protected final Logger logger = Logger.getLogger(getClass().getName());

	/*
	 * Constants
	 */
	private static final String DESCRIPTION = "Utility to download, replace and upload Google Drive binary files.";
	private static final String JAR_FILE = "drivecopy.jar";
	private static final String CONFIGURATION_FILE = "drivecopy.properties";

	// command line options
	private Options options = new Options();

	public Main(String[] args) {

		// compose options
		composeOptions();

		// create the command line parser
		CommandLineParser parser = new PosixParser();

		try {

			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			// check log option
			if (line.hasOption('L')) {

				// add file handler
				FileHandler handler = new FileHandler(line.getOptionValue('L'));
				handler.setLevel(Level.FINE);
				Logger.getLogger(getClass().getPackage().getName()).addHandler(handler);
				logger.info(String.format("Added log output file '%s'", line.getOptionValue('L')));
			}

			// check arguments number
			if (line.getArgs().length == 0)
				throw new ParseException("Missing argument MODE");

			// check mode
			int action = -1;
			if (line.getArgs()[0].equals("upload"))
				action = FileStorageWorkflowManager.ACTION_UPLOAD;
			else if (line.getArgs()[0].equals("download"))
				action = FileStorageWorkflowManager.ACTION_DOWNLOAD;
			else if (line.getArgs()[0].equals("replace"))
				action = FileStorageWorkflowManager.ACTION_REPLACE;
			if (action == -1)
				throw new ParseException("MODE must be 'download', 'replace' or 'upload'.");

			// compose BO
			FileBO fileBO = new FileBO();

			// check directory
			char c = 'f';
			fileBO.setDirectory(false);
			if (line.hasOption('d')) {

				c = 'd';
				fileBO.setDirectory(true);
			}
			fileBO.setFile(new File(line.getOptionValue(c)));

			// entry name
			if (line.getArgs().length == 2) {

				// check slashes
				String name = line.getArgs()[1];
				if (name.startsWith("/"))
					name = name.substring(1);
				if (name.endsWith("/"))
					name += "Untitled";
				fileBO.setName(name);

			} else
				fileBO.setName(fileBO.getFile().getName());

			// compression level
			fileBO.setCompressionLevel(Integer.parseInt(line.getOptionValue('l', "0")));

			// check delete after operation
			fileBO.setDeleteAfter(false);
			if (line.hasOption('D'))
				fileBO.setDeleteAfter(true);

			// check skip revision
			fileBO.setSkipRevision(false);
			if (line.hasOption('s'))
				fileBO.setSkipRevision(true);

			// MIME type
			if (line.hasOption('m'))
				fileBO.setMimeType(line.getOptionValue('m'));

			// check MD5 comparison
			fileBO.setCheckMd5(false);
			if (line.hasOption('c'))
				fileBO.setCheckMd5(true);

			// check force
			fileBO.setForce(false);
			if (line.hasOption('F'))
				fileBO.setForce(true);

			// check tree
			fileBO.setCreateFolders(false);
			if (line.hasOption('t'))
				fileBO.setCreateFolders(true);

			// get Workflow Manager
			Injector injector;
			if (line.hasOption('C'))
				injector = Guice.createInjector(new MainModule(line.getOptionValue('C')));
			else
				injector = Guice.createInjector(new MainModule(CONFIGURATION_FILE));
			FileStorageWorkflowManager wfm = injector.getInstance(FileStorageWorkflowManager.class);

			// execute workflow
			wfm.handleWorkflow(fileBO, action);

		} catch (ParseException ex) {

			// print help
			HelpFormatter formatter = new HelpFormatter();
			System.out.println("Drive Copy version " + VERSION);
			System.out.println("Copyright 2012-2015 Luca Zanconato (luca.zanconato@nharyes.net)");
			System.out.println();
			formatter.printHelp("java -jar " + JAR_FILE + " [OPTIONS] <MODE> [ENTRY]", DESCRIPTION + "\n", options, "\nMODE can be download/replace/upload.\nENTRY is the path of the entry in Google Drive (i.e. \"Test Folder/Another Folder/file.txt\"); if not set, the name of the local file/directory will be used.");
			System.out.println();

			// log exception
			logger.log(Level.SEVERE, ex.getMessage(), ex);

			// exit with error
			System.exit(1);

		} catch (Exception ex) {

			// log exception
			logger.log(Level.SEVERE, ex.getMessage(), ex);

			// exit with error
			System.exit(1);
		}
	}

	private void composeOptions() {

		// file option
		Option file = OptionBuilder.create('f');
		file.setLongOpt("file");
		file.setArgs(1);
		file.setArgName("path");
		file.setDescription("where path is the file to upload/download/replace.");

		// directory option
		Option directory = OptionBuilder.create('d');
		directory.setLongOpt("directory");
		directory.setArgs(1);
		directory.setArgName("path");
		directory.setDescription("where path is the local directory to upload/download/replace (it will be archived into a single remote file).");

		// file and directory group
		OptionGroup group = new OptionGroup();
		group.addOption(file);
		group.addOption(directory);
		group.setRequired(true);
		options.addOptionGroup(group);

		// compression level option
		Option level = OptionBuilder.create('l');
		level.setLongOpt("level");
		level.setArgs(1);
		level.setArgName("num");
		level.setOptionalArg(true);
		level.setType(Integer.class);
		level.setDescription("where num is the compression level from 0 to 9. Used when uploading/replacing directories. The default value is 0.");
		options.addOption(level);

		// delete option
		Option delete = OptionBuilder.create('D');
		delete.setLongOpt("delete");
		delete.setOptionalArg(true);
		delete.setType(Boolean.class);
		delete.setDescription("delete local file/directory after remote entry uploaded/replaced.");
		options.addOption(delete);

		// log option
		Option log = OptionBuilder.create('L');
		log.setLongOpt("log");
		log.setArgs(1);
		log.setArgName("file");
		log.setType(String.class);
		log.setDescription("where file is the log file to write");
		options.addOption(log);

		// MIME type option
		Option mimeType = OptionBuilder.create('m');
		mimeType.setLongOpt("mimetype");
		mimeType.setArgs(1);
		mimeType.setArgName("type");
		mimeType.setType(String.class);
		mimeType.setDescription("where type is the MIME type string to set for the remote entry. The default values are 'application/octet-stream' for files and 'application/zip' for compressed directories.");
		options.addOption(mimeType);

		// skip revision option
		Option skipRevision = OptionBuilder.create('s');
		skipRevision.setLongOpt("skiprevision");
		skipRevision.setOptionalArg(true);
		skipRevision.setType(Boolean.class);
		skipRevision.setDescription("do not create a new revision when replacing remote entry.");
		options.addOption(skipRevision);

		// check MD5 option
		Option checkMd5 = OptionBuilder.create('c');
		checkMd5.setLongOpt("checkmd5");
		checkMd5.setOptionalArg(true);
		checkMd5.setType(Boolean.class);
		checkMd5.setDescription("compare uploaded/downloaded local file MD5 summary with the one of the remote entry.");
		options.addOption(checkMd5);

		// check force creation option
		Option forceCreation = OptionBuilder.create('F');
		forceCreation.setLongOpt("force");
		forceCreation.setOptionalArg(true);
		forceCreation.setType(Boolean.class);
		forceCreation.setDescription("forces the creation of the remote entry when replace is selected and the entry doesn't exist.");
		options.addOption(forceCreation);

		// settings file option
		Option settings = OptionBuilder.create('C');
		settings.setLongOpt("configuration");
		settings.setArgs(1);
		settings.setArgName("path");
		settings.setType(String.class);
		settings.setDescription(String.format("where path is the path of the configuration file. The default value is '%s' in the same directory.", CONFIGURATION_FILE));
		options.addOption(settings);

		// create folders tree option
		Option tree = OptionBuilder.create('t');
		tree.setLongOpt("tree");
		tree.setOptionalArg(true);
		tree.setType(Boolean.class);
		tree.setDescription("create remote folders tree if one or more remote folders are not found.");
		options.addOption(tree);
	}

	public static void main(String[] args) {

		// set logger handler
		Logger logger = Logger.getLogger(Main.class.getPackage().getName());
		logger.setUseParentHandlers(false);
		logger.addHandler(new SystemOutHandler());
		logger.setLevel(Level.FINE);

		new Main(args);
	}
}
