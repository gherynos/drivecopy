/**
 * Copyright 2012 Luca Zanconato
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
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.nharyes.drivecopy.biz.bo.FileBO;
import net.nharyes.drivecopy.biz.exc.WorkflowManagerException;
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

	/**
	 * Logger.
	 */
	protected final Logger logger = Logger.getLogger(getClass().getName());
	
	/**
	 * Constants.
	 */
	private static final String DESCRIPTION = "Uploads, downloads and replaces files from and to Google Drive.";
	private static final String JAR_FILE = "drivecopy.jar";
	
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
		    if (line.hasOption('L'))
		    	Logger.getLogger("net.nharyes.drivecopy").addHandler(new FileHandler(line.getOptionValue('L')));
		    
		    // check arguments number
		    if (line.getArgs().length == 0) throw new ParseException("Missing arguments MODE and ENTRY.");
		    if (line.getArgs().length == 1) throw new ParseException("Missing argument ENTRY.");
		    
		    // check mode
		    int action = -1;
		    if (line.getArgs()[0].equals("upload")) action = FileStorageWorkflowManager.ACTION_UPLOAD;
		    else if (line.getArgs()[0].equals("download")) action = FileStorageWorkflowManager.ACTION_DOWNLOAD;
		    else if (line.getArgs()[0].equals("replace")) action = FileStorageWorkflowManager.ACTION_REPLACE;
		    if (action == -1) throw new ParseException("MODE must be 'download', 'replace' or 'upload'.");
		    
		    // compose BO
		    FileBO fileBO = new FileBO();
		    
		    // entry
		    fileBO.setName(line.getArgs()[1]);
		    
		    // check directory
		    char c = 'f';
		    fileBO.setDirectory(false);
		    if (line.hasOption('d')) {
		    	
		    	c = 'd';
		    	fileBO.setDirectory(true);
		    	
		    }
		    fileBO.setFile(new File(line.getOptionValue(c)));
			
		    // compression level
		    fileBO.setCompressionLevel(Integer.parseInt(line.getOptionValue('l', "0")));
		    
		    // check delete after operation
		    fileBO.setDeleteAfter(false);
		    if (line.hasOption('D')) fileBO.setDeleteAfter(true);
		    
		    // get Workflow Manager
		    Injector injector = Guice.createInjector(new MainModule());
			FileStorageWorkflowManager wfm = injector.getInstance(FileStorageWorkflowManager.class);
			
			// execute workflow
			wfm.handleWorkflow(fileBO, action);
		    
		} catch (ParseException ex) {
			
			// print help
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar "+JAR_FILE+" [OPTIONS] [MODE] [ENTRY]", DESCRIPTION+"\n", options, "\nMODE can be download/replace/upload.\nENTRY is the name of the entry in Google Drive.");
			System.out.println();
			
			// log exception
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			
			// exit with error
			System.exit(1);
			
		} catch (WorkflowManagerException ex) {
			
			// log exception
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			
			// exit with error
			System.exit(1);
			
		} catch (IOException ex) {
			
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
		directory.setDescription("where path is the directory to upload/download/replace (it will be archived into a single file).");
		
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
		delete.setDescription("delete file/directory after entry uploaded/replaced.");
		options.addOption(delete);
		
		// log option
		Option log = OptionBuilder.create('L');
		log.setLongOpt("log");
		log.setArgs(1);
		log.setArgName("file");
		log.setType(String.class);
		log.setDescription("where file is the log file to write");
		options.addOption(log);
	}
	
	public static void main(String[] args) {
	    
		// set logger handler
		Logger.getLogger("net.nharyes.drivecopy").setUseParentHandlers(false);
		Logger.getLogger("net.nharyes.drivecopy").addHandler(new SystemOutHandler());
		
		new Main(args);
	}
}
