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

package net.nharyes.drivecopy.mod;

import net.nharyes.drivecopy.biz.wfm.DirectoryCompressorWorkflowManager;
import net.nharyes.drivecopy.biz.wfm.DirectoryCompressorWorkflowManagerImpl;
import net.nharyes.drivecopy.biz.wfm.FileStorageWorkflowManager;
import net.nharyes.drivecopy.biz.wfm.FileStorageWorkflowManagerImpl;
import net.nharyes.drivecopy.biz.wfm.TokenWorkflowManager;
import net.nharyes.drivecopy.biz.wfm.TokenWorkflowManagerImpl;
import net.nharyes.drivecopy.srvc.DriveSdo;
import net.nharyes.drivecopy.srvc.DriveSdoImpl;
import net.nharyes.drivecopy.srvc.TokenSdo;
import net.nharyes.drivecopy.srvc.TokenSdoImpl;

import com.google.inject.AbstractModule;

public class MainModule extends AbstractModule {

	@Override
	protected void configure() {

		// Docs SDO
		bind(DriveSdo.class).to(DriveSdoImpl.class);

		// Token SDO
		bind(TokenSdo.class).to(TokenSdoImpl.class);

		// File Storage Workflow Manager
		bind(FileStorageWorkflowManager.class).to(FileStorageWorkflowManagerImpl.class);

		// Directory Compressor Workflow Manager
		bind(DirectoryCompressorWorkflowManager.class).to(DirectoryCompressorWorkflowManagerImpl.class);
		
		// Token Workflow Manager
		bind(TokenWorkflowManager.class).to(TokenWorkflowManagerImpl.class);
	}
}
