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

package net.nharyes.drivecopy.biz.wfm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.nharyes.drivecopy.biz.bo.DirectoryBO;
import net.nharyes.drivecopy.biz.exc.WorkflowManagerException;

import com.google.inject.Singleton;

@Singleton
public class DirectoryCompressorWorkflowManagerImpl extends BaseWorkflowManager<DirectoryBO> implements DirectoryCompressorWorkflowManager {

	/**
	 * Constants
	 */
	private final int BUFFER = 2048;

	@Override
	public DirectoryBO handleWorkflow(DirectoryBO businessObject, int action) throws WorkflowManagerException {

		switch (action) {

		case ACTION_COMPRESS:
			return compress(businessObject);
		case ACTION_DECOMPRESS:
			return decompress(businessObject);
		default:
			throw new WorkflowManagerException("Action not found");
		}
	}

	private DirectoryBO compress(DirectoryBO directory) throws WorkflowManagerException {

		try {

			// log action
			logger.fine("Compress directory");

			// create temporary file
			File tempFile = File.createTempFile("cloudmirror" + System.currentTimeMillis(), "temp");

			// create output stream
			BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(tempFile));
			ZipOutputStream zout = new ZipOutputStream(bout);
			zout.setLevel(directory.getLevel());

			// process file and subdirectories
			DirectoryBO dirBO = new DirectoryBO();
			String filePath = directory.getFile().getAbsolutePath();
			processFile(new File(filePath), zout, filePath.substring(0, filePath.lastIndexOf(File.separator)), dirBO.getNotCompressed());

			// close output stream
			zout.close();

			// return created file
			dirBO.setFile(tempFile);
			dirBO.setLevel(directory.getLevel());
			return dirBO;

		} catch (IOException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);
		}
	}

	private DirectoryBO decompress(DirectoryBO directory) throws WorkflowManagerException {

		try {

			// output stream
			FileInputStream fin = new FileInputStream(directory.getFile());
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fin));

			// process zip entries
			int count;
			byte data[] = new byte[BUFFER];
			ZipEntry entry;
			BufferedOutputStream dest = null;
			while ((entry = zis.getNextEntry()) != null) {

				// status
				logger.info(String.format("decompressing '%s'", entry.getName().substring(entry.getName().lastIndexOf(File.separator) + 1)));

				// eventually create subdirectories for file
				String f = directory.getDestinationDirectory().getAbsolutePath() + File.separator + entry.getName();
				File fl = new File(f);
				if (entry.getName().indexOf(File.separator) != -1) {

					new File(f.substring(0, f.lastIndexOf(File.separator))).mkdirs();
				}

				// check if file can be written
				if (!fl.createNewFile()) {

					// notify UI
					logger.warning(String.format("unable to decompress '%s'", fl.getAbsolutePath()));

					// read entry from stream
					while ((count = zis.read(data, 0, BUFFER)) != -1) {
					}

				} else {

					// write the file to the disk
					FileOutputStream fos = new FileOutputStream(fl);
					dest = new BufferedOutputStream(fos, BUFFER);
					while ((count = zis.read(data, 0, BUFFER)) != -1) {

						dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();
				}
			}
			zis.close();

			// return the same BO
			return directory;

		} catch (IOException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);
		}
	}

	private void processFile(File f, ZipOutputStream zout, String path, List<File> notCompressed) throws IOException {

		// check if file can be read
		if (!f.canRead()) {

			// notify UI
			logger.warning(String.format("unable to compress '%s'", f.getAbsolutePath()));

			// add to not compressed files
			notCompressed.add(f);

			return;
		}

		byte data[] = new byte[BUFFER];

		// check if file is directory
		if (f.isDirectory()) {

			// process all files contained
			File[] files = f.listFiles();
			for (File fl : files)
				processFile(fl, zout, path, notCompressed);

		} else {

			// extract entry name
			String entryName = f.getAbsolutePath().substring(f.getAbsolutePath().indexOf(path) + path.length() + 1);

			// status
			logger.info(String.format("compressing '%s'", f.getName()));

			// create input stream
			BufferedInputStream bin = new BufferedInputStream(new FileInputStream(f), BUFFER);

			// create entry
			ZipEntry entry = new ZipEntry(entryName);
			zout.putNextEntry(entry);
			int count;
			while ((count = bin.read(data, 0, BUFFER)) != -1) {

				zout.write(data, 0, count);
			}
			bin.close();
		}
	}
}
