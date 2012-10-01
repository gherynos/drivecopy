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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.biz.exc.WorkflowManagerException;
import net.nharyes.drivecopy.srvc.TokenSdo;
import net.nharyes.drivecopy.srvc.exc.SdoException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TokenWorkflowManagerImpl extends BaseWorkflowManager<TokenBO> implements TokenWorkflowManager {

	// configuration file
	private final String configFile = "drivecopy.properties";

	// token SDO
	private TokenSdo tokenSdo;

	@Inject
	public TokenWorkflowManagerImpl(TokenSdo tokenSdo) {

		this.tokenSdo = tokenSdo;
	}

	@Override
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

			// verify configuration file existence
			File s = new File(configFile);
			if (s.exists()) {

				// load settings
				Properties sets = new Properties();
				sets.load(new FileInputStream(s));
				return new TokenBO(sets.getProperty("accessToken"), sets.getProperty("refreshToken"));
			}

			// request token
			TokenBO tk = tokenSdo.requestToken();

			// store token into properties
			Properties sets = new Properties();
			sets.setProperty("accessToken", tk.getAccessToken());
			sets.setProperty("refreshToken", tk.getRefreshToken());
			FileOutputStream fout = new FileOutputStream(s);
			sets.store(fout, "Cloud Mirror authentication token");
			fout.flush();
			fout.close();

			return tk;

		} catch (IOException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);

		} catch (SdoException ex) {

			// re-throw exception
			throw new WorkflowManagerException(ex.getMessage(), ex);
		}
	}
}
