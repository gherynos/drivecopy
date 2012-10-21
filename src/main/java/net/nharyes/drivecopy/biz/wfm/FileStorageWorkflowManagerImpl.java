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

import java.io.File;
import java.io.IOException;
import java.util.List;

import net.nharyes.drivecopy.biz.bo.DirectoryBO;
import net.nharyes.drivecopy.biz.bo.EntryBO;
import net.nharyes.drivecopy.biz.bo.FileBO;
import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.biz.exc.WorkflowManagerException;
import net.nharyes.drivecopy.srvc.DriveSdo;
import net.nharyes.drivecopy.srvc.exc.ItemNotFoundException;
import net.nharyes.drivecopy.srvc.exc.SdoException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileStorageWorkflowManagerImpl extends BaseWorkflowManager<FileBO> implements FileStorageWorkflowManager {

	// Drive SDO
	private DriveSdo driveSdo;

	// Directory Compressor WFM
	private DirectoryCompressorWorkflowManager directoryCompressorWorkflowManager;

	// Token WFM
	private TokenWorkflowManager tokenWorkflowManager;

	@Inject
	public FileStorageWorkflowManagerImpl(DriveSdo driveSdo, DirectoryCompressorWorkflowManager directoryCompressorWorkflowManager, TokenWorkflowManager tokenWorkflowManager) {

		this.driveSdo = driveSdo;
		this.directoryCompressorWorkflowManager = directoryCompressorWorkflowManager;
		this.tokenWorkflowManager = tokenWorkflowManager;
	}

	@Override
	public FileBO handleWorkflow(FileBO businessObject, int action) throws WorkflowManagerException {

		switch (action) {

		case ACTION_UPLOAD:
			return upload(businessObject);
		case ACTION_DOWNLOAD:
			return download(businessObject);
		case ACTION_REPLACE:
			return replace(businessObject);
		default:
			throw new WorkflowManagerException("Action not found");
		}
	}

	private TokenBO getToken() throws WorkflowManagerException {

		return tokenWorkflowManager.handleWorkflow(new TokenBO(), TokenWorkflowManager.ACTION_GET);
	}

	private FileBO upload(FileBO file) throws WorkflowManagerException {

		try {

			// get token
			TokenBO token = getToken();

			// log action
			logger.info(String.format("Upload file '%s' to entry '%s'", file.getFile().getAbsolutePath(), file.getName()));

			try {

				// search entry
				driveSdo.searchEntry(token, file.getName());

				// entry already exists
				throw new SdoException(String.format("Entry with name '%s' already exists", file.getName()));

			} catch (ItemNotFoundException ex) {

				// compose BO
				EntryBO doc = new EntryBO();
				doc.setFile(file.getFile());
				doc.setName(file.getName());
				doc.setMimeType(file.getMimeType());

				// check directory
				DirectoryBO dirBO = new DirectoryBO();
				if (file.isDirectory()) {

					// compress directory
					logger.info(String.format("Compress directory with level '%d'", file.getCompressionLevel()));
					dirBO.setFile(file.getFile());
					dirBO.setLevel(file.getCompressionLevel());
					dirBO = directoryCompressorWorkflowManager.handleWorkflow(dirBO, DirectoryCompressorWorkflowManager.ACTION_COMPRESS);

					// replace file
					doc.setFile(dirBO.getFile());

					// eventually set ZIP MIME type
					if (doc.getMimeType() == null)
						doc.setMimeType("application/zip");

				} else {

					// eventually set generic MIME type
					if (doc.getMimeType() == null)
						doc.setMimeType("application/octet-stream");
				}

				// upload entry
				logger.info(String.format("MIME type of the entry: %s", doc.getMimeType()));
				doc = driveSdo.uploadEntry(token, doc);

				// eventually delete file or directory
				if (file.isDeleteAfter()) {

					logger.info("Process file(s) for deletion...");
					processFileForDeletion(file.getFile(), dirBO.getNotCompressed());
				}

				// return uploaded entry
				FileBO fBO = new FileBO();
				fBO.setFile(doc.getFile());
				fBO.setName(doc.getName());
				return fBO;
			}

		} catch (SdoException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);

		} catch (IOException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);
		}
	}

	private FileBO download(FileBO file) throws WorkflowManagerException {

		try {

			// get token
			TokenBO token = getToken();

			// log action
			logger.info(String.format("Download entry '%s' to file '%s'", file.getName(), file.getFile().getAbsolutePath()));

			// check delete after option
			if (file.isDeleteAfter())
				logger.warning("Delete option ignored");

			// check MIME type option
			if (file.getMimeType() != null)
				logger.warning("MIME type option ignored");

			// search entry
			EntryBO entry = driveSdo.searchEntry(token, file.getName());

			// check directory
			if (file.isDirectory()) {

				// create temporary file
				File tempFile = File.createTempFile("drivecopy" + System.currentTimeMillis(), "temp");
				logger.fine(String.format("Created temporary file '%s'", tempFile.getAbsolutePath()));

				// set file property
				entry.setFile(tempFile);

			} else {

				// set file property
				entry.setFile(file.getFile());
			}

			// download entry
			entry = driveSdo.downloadEntry(token, entry);

			// check directory
			if (file.isDirectory()) {

				logger.info("Decompress file");

				// decompress file
				DirectoryBO dirBO = new DirectoryBO();
				dirBO.setFile(entry.getFile());
				dirBO.setDestinationDirectory(file.getFile());
				dirBO = directoryCompressorWorkflowManager.handleWorkflow(dirBO, DirectoryCompressorWorkflowManager.ACTION_DECOMPRESS);

				// delete downloaded file
				logger.fine("Delete downloaded file");
				entry.getFile().delete();

				// return decompressed directory
				FileBO fBO = new FileBO();
				fBO.setFile(dirBO.getFile());
				fBO.setName(entry.getName());
				return fBO;
			}

			// return downloaded entry
			FileBO fBO = new FileBO();
			fBO.setFile(entry.getFile());
			fBO.setName(entry.getName());
			return fBO;

		} catch (SdoException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);

		} catch (IOException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);
		}
	}

	private FileBO replace(FileBO file) throws WorkflowManagerException {

		try {

			// get token
			TokenBO token = getToken();

			// log action
			logger.info(String.format("Replace entry '%s' with file '%s'", file.getName(), file.getFile().getAbsolutePath()));

			// search entry
			EntryBO entry = driveSdo.searchEntry(token, file.getName());

			// set file property
			entry.setFile(file.getFile());

			// set MIME type property
			entry.setMimeType(file.getMimeType());

			// check directory
			DirectoryBO dirBO = new DirectoryBO();
			if (file.isDirectory()) {

				// compress directory
				logger.info(String.format("Compress directory with level '%d'", file.getCompressionLevel()));
				dirBO.setFile(file.getFile());
				dirBO.setLevel(file.getCompressionLevel());
				dirBO = directoryCompressorWorkflowManager.handleWorkflow(dirBO, DirectoryCompressorWorkflowManager.ACTION_COMPRESS);

				// replace file
				entry.setFile(dirBO.getFile());

				// eventually set ZIP MIME type
				if (entry.getMimeType() == null)
					entry.setMimeType("application/zip");

			} else {

				// eventually set generic MIME type
				if (entry.getMimeType() == null)
					entry.setMimeType("application/octet-stream");
			}

			// replace entry
			logger.info(String.format("MIME type of the entry: %s", entry.getMimeType()));
			entry = driveSdo.updateEntry(token, entry);

			// eventually delete temporary file
			if (file.isDirectory()) {

				logger.fine("Delete temporary file");
				entry.getFile().delete();
			}

			// eventually delete file or directory
			if (file.isDeleteAfter()) {

				logger.info("Process file(s) for deletion...");
				processFileForDeletion(file.getFile(), dirBO.getNotCompressed());
			}

			// return updated entry
			FileBO fBO = new FileBO();
			fBO.setFile(entry.getFile());
			fBO.setName(entry.getName());
			return fBO;

		} catch (SdoException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);

		} catch (IOException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);
		}
	}

	private void processFileForDeletion(File f, List<File> notCompressed) throws IOException {

		// check if file not compressed or cannot be read or written
		if (notCompressed.contains(f) || !f.canRead() || !f.canWrite()) {

			// notify UI
			logger.warning(String.format("File '%s' not deleted", f.getAbsolutePath()));

			return;
		}

		// check if file is directory
		if (f.isDirectory()) {

			// process all files contained
			logger.fine(String.format("Process directory '%s' for deletion", f.getAbsolutePath()));
			File[] files = f.listFiles();
			for (File fl : files)
				processFileForDeletion(fl, notCompressed);
		}

		// delete file/directory
		logger.fine(String.format("Delete file/directory '%s'", f.getAbsolutePath()));
		f.delete();
	}
}
