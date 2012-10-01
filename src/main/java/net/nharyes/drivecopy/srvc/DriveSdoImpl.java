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

package net.nharyes.drivecopy.srvc;

import java.io.FileOutputStream;
import java.io.IOException;

import net.nharyes.drivecopy.biz.bo.EntryBO;
import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.biz.exc.WorkflowManagerException;
import net.nharyes.drivecopy.srvc.exc.ItemNotFoundException;
import net.nharyes.drivecopy.srvc.exc.SdoException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.inject.Singleton;

@Singleton
public class DriveSdoImpl implements DriveSdo {

	/**
	 * Constants
	 */
	private static String CLIENT_ID = "[CLIENT_ID]";
	private static String CLIENT_SECRET = "[CLIENT_SECRET]";

	private HttpTransport httpTransport;

	private JsonFactory jsonFactory;

	public DriveSdoImpl() throws WorkflowManagerException {

		httpTransport = new NetHttpTransport();
		jsonFactory = new com.google.api.client.json.jackson2.JacksonFactory();
	}

	private Drive getService(TokenBO token) {

		GoogleCredential credential = new GoogleCredential.Builder().setClientSecrets(CLIENT_ID, CLIENT_SECRET).setJsonFactory(jsonFactory).setTransport(httpTransport).build().setRefreshToken(token.getRefreshToken()).setAccessToken(token.getAccessToken());
		return new Drive.Builder(httpTransport, jsonFactory, credential).build();
	}

	@Override
	public EntryBO downloadEntry(TokenBO token, EntryBO entry) throws SdoException {

		try {

			// get file
			Drive service = getService(token);
			File file = service.files().get(entry.getId()).execute();

			// check download URL and size
			if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {

				// download file
				HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
				FileOutputStream fout = new FileOutputStream(entry.getFile());
				resp.download(fout);
				fout.flush();
				fout.close();

				// return the same entry
				return entry;

			} else {

				// the file doesn't have any content stored on Drive
				throw new ItemNotFoundException(String.format("File with id '%s' doesn't have any content stored on Drive", entry.getId()));
			}

		} catch (IOException ex) {

			// re-throw exception
			throw new SdoException(ex.getMessage(), ex);
		}
	}

	@Override
	public EntryBO uploadEntry(TokenBO token, EntryBO entry) throws SdoException {

		try {

			// create file item
			File body = new File();
			body.setTitle(entry.getName());
			body.setMimeType("application/octet-stream");

			// set content
			FileContent mediaContent = new FileContent("application/octet-stream", entry.getFile());

			// upload file
			File file = getService(token).files().insert(body, mediaContent).execute();

			// compose output entry
			EntryBO entryBO = new EntryBO();
			entryBO.setId(file.getId());
			entryBO.setFile(entry.getFile());
			return entryBO;

		} catch (IOException ex) {

			// re-throw exception
			throw new SdoException(ex.getMessage(), ex);
		}
	}

	@Override
	public EntryBO updateEntry(TokenBO token, EntryBO entry) throws SdoException {

		try {

			// get file
			Drive service = getService(token);
			File file = service.files().get(entry.getId()).execute();

			// update file content
			FileContent mediaContent = new FileContent(file.getMimeType(), entry.getFile());

			// update file
			File updatedFile = service.files().update(entry.getId(), file, mediaContent).execute();

			// compose output entry
			EntryBO docBO = new EntryBO();
			docBO.setId(updatedFile.getId());
			docBO.setName(updatedFile.getTitle());
			docBO.setFile(entry.getFile());
			return docBO;

		} catch (IOException ex) {

			// re-throw exception
			throw new SdoException(ex.getMessage(), ex);
		}
	}

	@Override
	public EntryBO searchEntry(TokenBO token, String name) throws SdoException {

		try {

			// compose list query
			Files.List request = getService(token).files().list();
			request.setQ("title contains '" + name + "'");
			request.setMaxResults(2);

			// execute query
			FileList files = request.execute();

			// check no results
			if (files.getItems().isEmpty())
				throw new ItemNotFoundException(String.format("No file found with name '%s'", name));

			// check multiple results
			if (files.getItems().size() > 1)
				throw new SdoException(String.format("Multiple results for entry with name '%s'", name));

			// check exact title
			File file = files.getItems().get(0);
			if (!file.getTitle().equals(name))
				throw new ItemNotFoundException(String.format("No file found with name '%s'", name));

			// return entry
			EntryBO entry = new EntryBO();
			entry.setId(file.getId());
			entry.setName(file.getTitle());
			return entry;

		} catch (IOException ex) {

			// re-throw exception
			throw new SdoException(ex.getMessage(), ex);
		}
	}
}
