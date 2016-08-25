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

package net.nharyes.drivecopy.srvc;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.*;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.Drive.Files.Update;
import com.google.api.services.drive.DriveRequest;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.nharyes.drivecopy.biz.bo.EntryBO;
import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.srvc.exc.FolderNotFoundException;
import net.nharyes.drivecopy.srvc.exc.ItemNotFoundException;
import net.nharyes.drivecopy.srvc.exc.SdoException;

import javax.annotation.Nonnull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class DriveSdoImpl implements DriveSdo {

	/*
	 * Logger
	 */
	protected final Logger logger = Logger.getLogger(getClass().getName());

	/*
	 * HTTP Request read and connect timeout
	 */
	private static final int HTTP_REQUEST_TIMEOUT = 3 * 600000;

	// HTTP transport
	private HttpTransport httpTransport;

	// JSON factory
	private JsonFactory jsonFactory;

	// File upload progress listener
	private MediaHttpUploaderProgressListener fileUploadProgressListener;

	// File download progress listener
	private MediaHttpDownloaderProgressListener fileDownloadProgressListener;

	@Inject
	public DriveSdoImpl(HttpTransport httpTransport, JsonFactory jsonFactory, MediaHttpUploaderProgressListener fileUploadProgressListener, MediaHttpDownloaderProgressListener fileDownloadProgressListener) {

		this.httpTransport = httpTransport;
		this.jsonFactory = jsonFactory;
		this.fileUploadProgressListener = fileUploadProgressListener;
		this.fileDownloadProgressListener = fileDownloadProgressListener;
	}

	private Drive getService(@Nonnull TokenBO token) {

		final GoogleCredential credential = new GoogleCredential.Builder().setClientSecrets(token.getClientId(), token.getClientSecret()).setJsonFactory(jsonFactory).setTransport(httpTransport).build().setRefreshToken(token.getRefreshToken()).setAccessToken(token.getAccessToken());

		return new Drive.Builder(httpTransport, jsonFactory, new HttpRequestInitializer() {

			public void initialize(HttpRequest httpRequest) {

				try {

					// initialize credentials
					credential.initialize(httpRequest);

					// set connect and read timeouts
					httpRequest.setConnectTimeout(HTTP_REQUEST_TIMEOUT);
					httpRequest.setReadTimeout(HTTP_REQUEST_TIMEOUT);

				} catch (IOException ex) {

					// log exception
					logger.log(Level.SEVERE, ex.getMessage(), ex);
				}
			}
		}).setApplicationName("DriveCopy").build();
	}

	private <T> T executeWithExponentialBackoff(DriveRequest<T> req) throws IOException, InterruptedException {

		Random randomGenerator = new Random();

		for (int n = 0; n < 5; ++n) {

			try {

				return req.execute();

			} catch (GoogleJsonResponseException e) {

				if (e.getStatusCode() == 503 || e.getStatusCode() == 500 || (e.getStatusCode() == 403 && (e.getDetails().getErrors().get(0).getReason().equals("rateLimitExceeded") || e.getDetails().getErrors().get(0).getReason().equals("userRateLimitExceeded")))) {

					// apply exponential backoff.
					Thread.sleep((1 << n) * 1000 + randomGenerator.nextInt(1001));

				} else {

					// other error, re-throw.
					throw e;
				}
			}
		}

		throw new IOException("There has been an error, the request never succeeded.");
	}

	public EntryBO downloadEntry(@Nonnull TokenBO token, @Nonnull EntryBO entry) throws SdoException {

		try {

			// get file
			Drive service = getService(token);
			Get get = service.files().get(entry.getId());
			MediaHttpDownloader downloader = new MediaHttpDownloader(httpTransport, service.getRequestFactory().getInitializer());
			downloader.setDirectDownloadEnabled(false);
			downloader.setProgressListener(fileDownloadProgressListener);
			File file = executeWithExponentialBackoff(get);

			// check download URL and size
			if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {

				// download file
				FileOutputStream fout = new FileOutputStream(entry.getFile());
				downloader.download(new GenericUrl(file.getDownloadUrl()), fout);
				fout.flush();
				fout.close();

				// return entry
				entry.setMd5Sum(file.getMd5Checksum());
				return entry;

			} else {

				// the file doesn't have any content stored on Drive
				throw new ItemNotFoundException(String.format("Remote file with id '%s' doesn't have any content stored on Drive", entry.getId()));
			}

		} catch (IOException | InterruptedException ex) {

			// re-throw exception
			throw new SdoException(ex.getMessage(), ex);
		}
	}

	public EntryBO uploadEntry(@Nonnull TokenBO token, @Nonnull EntryBO entry, @Nonnull String parentId) throws SdoException {

		try {

			// create file item
			File body = new File();
			body.setTitle(entry.getName());
			body.setMimeType(entry.getMimeType());

			// set parent
            ParentReference newParent = new ParentReference();
            newParent.setId(parentId);
            body.setParents(new ArrayList<ParentReference>());
            body.getParents().add(newParent);

			// set content
			FileContent mediaContent = new FileContent(entry.getMimeType(), entry.getFile());

			// upload file
			Insert insert = getService(token).files().insert(body, mediaContent);
			MediaHttpUploader uploader = insert.getMediaHttpUploader();
			uploader.setDirectUploadEnabled(false);
			uploader.setProgressListener(fileUploadProgressListener);
			File file = executeWithExponentialBackoff(insert);

			// compose output entry
			EntryBO entryBO = new EntryBO();
			entryBO.setId(file.getId());
			entryBO.setFile(entry.getFile());
			entryBO.setMd5Sum(file.getMd5Checksum());
			return entryBO;

		} catch (IOException | InterruptedException ex) {

			// re-throw exception
			throw new SdoException(ex.getMessage(), ex);
		}
	}

	public EntryBO updateEntry(@Nonnull TokenBO token, @Nonnull EntryBO entry) throws SdoException {

		try {

			// get file
			Drive service = getService(token);
			File file = executeWithExponentialBackoff(service.files().get(entry.getId()));

			// update file content
			FileContent mediaContent = new FileContent(entry.getMimeType(), entry.getFile());
			file.setMimeType(entry.getMimeType());

			// update file
			Update update = service.files().update(entry.getId(), file, mediaContent);
			update.setNewRevision(!entry.isSkipRevision());
			MediaHttpUploader uploader = update.getMediaHttpUploader();
			uploader.setDirectUploadEnabled(false);
			uploader.setProgressListener(fileUploadProgressListener);
			File updatedFile = executeWithExponentialBackoff(update);

			// compose output entry
			EntryBO docBO = new EntryBO();
			docBO.setId(updatedFile.getId());
			docBO.setName(updatedFile.getTitle());
			docBO.setFile(entry.getFile());
			docBO.setMd5Sum(updatedFile.getMd5Checksum());
			return docBO;

		} catch (IOException | InterruptedException ex) {

			// re-throw exception
			throw new SdoException(ex.getMessage(), ex);
		}
	}

	public String getLastFolderId(@Nonnull TokenBO token, String[] folders, @Nonnull String rootId, boolean createIfNotFound) throws SdoException {

		Drive service = getService(token);

		try {

			// check folders
			String lastParentId = rootId;
			String lastParentName = null;
			if (folders != null) {

				// check folders existence
				for (String currentFolder : folders) {

					try {

						// compose current folder query
						Files.List request = service.files().list();
						request.setQ(String.format("title = '%s' and trashed = false and mimeType = 'application/vnd.google-apps.folder' and '%s' in parents", currentFolder, lastParentId));
						request.setMaxResults(2);

						// execute query
						logger.fine(String.format("Search remote folder with name '%s'", currentFolder));
						FileList fs = executeWithExponentialBackoff(request);

						// check no results
						if (fs.getItems().isEmpty())
							throw new FolderNotFoundException(String.format("No remote folder found with name '%s'%s", currentFolder, lastParentName != null ? String.format(" in remote folder '%s'", lastParentName) : ""));

						// check multiple results
						if (fs.getItems().size() > 1)
							throw new SdoException(String.format("Multiple results for remote folder with name '%s'%s", currentFolder, lastParentName != null ? String.format(" in remote folder '%s'", lastParentName) : ""));

						// check exact title
						File folder = fs.getItems().get(0);
						if (!folder.getTitle().equals(currentFolder))
							throw new FolderNotFoundException(String.format("No remote folder found with exact name '%s'%s", currentFolder, lastParentName != null ? String.format(" in remote folder '%s'", lastParentName) : ""));

						// set parent ID for next folder/file
						lastParentId = folder.getId();
						lastParentName = folder.getTitle();

					} catch (FolderNotFoundException ex) {

						// in case re-throw exception
						if (!createIfNotFound)
							throw ex;

						// create folder
						logger.fine(String.format("Create remote folder with name '%s'", currentFolder));
						File folder = new File();
						folder.setTitle(currentFolder);
						folder.setMimeType("application/vnd.google-apps.folder");
						folder.setParents(Collections.singletonList(new ParentReference().setId(lastParentId != null ? lastParentId : "root")));
						folder = executeWithExponentialBackoff(service.files().insert(folder));

						// set parent ID for next folder/file
						lastParentId = folder.getId();
						lastParentName = folder.getTitle();
					}
				}
			}

			return lastParentId;

		} catch (IOException | InterruptedException ex) {

			// re-throw exception
			throw new SdoException(ex.getMessage(), ex);
		}
	}

	public EntryBO searchEntry(@Nonnull TokenBO token, @Nonnull String name, @Nonnull String parentId) throws SdoException {

		try {

			// compose list query
			Files.List request = getService(token).files().list();
			request.setQ(String.format("title = '%s' and trashed = false and mimeType != 'application/vnd.google-apps.folder' and '%s' in parents", name, parentId));
			request.setMaxResults(2);

			// execute query
			logger.fine(String.format("Search entry with name '%s'", name));
			FileList files = executeWithExponentialBackoff(request);

			// check no results
			if (files.getItems().isEmpty())
				throw new ItemNotFoundException(String.format("No remote file found with name '%s'", name));

			// check multiple results
			if (files.getItems().size() > 1)
				throw new SdoException(String.format("Multiple results for entry with name '%s'", name));

			// check exact title
			File file = files.getItems().get(0);
			if (!file.getTitle().equals(name))
				throw new ItemNotFoundException(String.format("No remote file found with exact name '%s'", name));

			// return entry
			EntryBO entry = new EntryBO();
			entry.setId(file.getId());
			entry.setName(file.getTitle());
			return entry;

		} catch (IOException | InterruptedException ex) {

			// re-throw exception
			throw new SdoException(ex.getMessage(), ex);
		}
	}
}
