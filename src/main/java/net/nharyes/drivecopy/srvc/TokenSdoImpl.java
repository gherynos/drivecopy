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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.srvc.exc.SdoException;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.inject.Singleton;

@Singleton
public class TokenSdoImpl implements TokenSdo {

	/**
	 * Constants
	 */
	private static String CLIENT_ID = "[CLIENT_ID]";
	private static String CLIENT_SECRET = "[CLIENT_SECRET]";
	private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

	@Override
	public TokenBO requestToken() throws SdoException {

		try {

			// create transport and JSON factory
			HttpTransport httpTransport = new NetHttpTransport();
			JsonFactory jsonFactory = new JacksonFactory();

			// create authorization flow
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE)).build();

			// request authorization to user
			String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
			System.out.println("Please open the following URL in your browser then type the authorization code:");
			System.out.println("  " + url);
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String code = br.readLine();

			// process response
			GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
			Credential credential = flow.createAndStoreCredential(response, null);
			
			// return token
			return new TokenBO(credential.getAccessToken(), credential.getRefreshToken());

		} catch (IOException ex) {

			// re-throw exception
			throw new SdoException(ex.getMessage(), ex);
		}
	}
}
