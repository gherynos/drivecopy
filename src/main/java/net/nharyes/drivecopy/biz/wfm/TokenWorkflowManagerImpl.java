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

package net.nharyes.drivecopy.biz.wfm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;

import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.biz.exc.WorkflowManagerException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TokenWorkflowManagerImpl extends BaseWorkflowManager<TokenBO> implements TokenWorkflowManager {

	/*
	 * Constants
	 */
	private final String CLIENT_ID_KEY = "clientId";
	private final String CLIENT_SECRET_KEY = "clientSecret";
	private final String ACCESS_TOKEN_KEY = "accessToken";
	private final String REFRESH_TOKEN_KEY = "refreshToken";
	private final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

	// configuration
	private PropertiesConfiguration config;

	// HTTP transport
	private HttpTransport httpTransport;

	// JSON factory
	private JsonFactory jsonFactory;

	@Inject
	public TokenWorkflowManagerImpl(PropertiesConfiguration config, HttpTransport httpTransport, JsonFactory jsonFactory) {

		this.config = config;
		this.httpTransport = httpTransport;
		this.jsonFactory = jsonFactory;
	}

	public TokenBO handleWorkflow(TokenBO businessObject, int action) throws WorkflowManagerException {

		switch (action) {

		case ACTION_GET:
			return get(businessObject);
		default:
			throw new WorkflowManagerException("Action not found");
		}
	}

	private TokenBO get(TokenBO token) throws WorkflowManagerException {

		try {

			// check client ID and client secret configuration existence
			if (!config.containsKey(CLIENT_ID_KEY) || !config.containsKey(CLIENT_SECRET_KEY)) {

				// request client data to user
				System.out.println("Configuration file not found; generating a new one...");
				System.out.println("(see https://github.com/Gherynos/DriveCopy/wiki/Setup for help)");
				System.out.println();
				System.out.println("Please insert CLIENT ID:");
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String clientId = br.readLine();
				System.out.println("Please insert CLIENT SECRET:");
				String clientSecret = br.readLine();

				// store client data
				config.setProperty(CLIENT_ID_KEY, clientId);
				config.setProperty(CLIENT_SECRET_KEY, clientSecret);
				config.save();
			}

			// check tokens configuration existence
			if (!config.containsKey(ACCESS_TOKEN_KEY) || !config.containsKey(REFRESH_TOKEN_KEY)) {

				// request authorization to user
				GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, config.getString(CLIENT_ID_KEY), config.getString(CLIENT_SECRET_KEY), Collections.singletonList(DriveScopes.DRIVE)).build();
				String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
				System.out.println("Please open the following URL in your browser then type the authorization code:");
				System.out.println("  " + url);
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String code = br.readLine();

				// process response
				GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
				Credential credential = flow.createAndStoreCredential(response, null);

				// store tokens
				config.setProperty(ACCESS_TOKEN_KEY, credential.getAccessToken());
				config.setProperty(REFRESH_TOKEN_KEY, credential.getRefreshToken());
				config.save();
			}

			// return token
			return new TokenBO(config.getString(CLIENT_ID_KEY), config.getString(CLIENT_SECRET_KEY), config.getString(ACCESS_TOKEN_KEY), config.getString(REFRESH_TOKEN_KEY));

		} catch (IOException | ConfigurationException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);
		}
	}
}
