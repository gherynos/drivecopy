/**
 * Copyright 2012-2013 Luca Zanconato
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

import net.nharyes.drivecopy.FileDownloadProgressListener;
import net.nharyes.drivecopy.FileUploadProgressListener;
import net.nharyes.drivecopy.biz.wfm.DirectoryCompressorWorkflowManager;
import net.nharyes.drivecopy.biz.wfm.DirectoryCompressorWorkflowManagerImpl;
import net.nharyes.drivecopy.biz.wfm.FileStorageWorkflowManager;
import net.nharyes.drivecopy.biz.wfm.FileStorageWorkflowManagerImpl;
import net.nharyes.drivecopy.biz.wfm.TokenWorkflowManager;
import net.nharyes.drivecopy.biz.wfm.TokenWorkflowManagerImpl;
import net.nharyes.drivecopy.srvc.DriveSdo;
import net.nharyes.drivecopy.srvc.DriveSdoImpl;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class MainModule extends AbstractModule {

	/*
	 * Configuration file
	 */
	private String configFile;

	public MainModule(String configFile) {

		super();

		// set configuration file
		this.configFile = configFile;
	}

	@Provides
	@Singleton
	private PropertiesConfiguration providePropertiesConfiguration() {

		PropertiesConfiguration config;

		try {

			// load configuration from file
			config = new PropertiesConfiguration(configFile);

		} catch (ConfigurationException ex) {

			// create empty configuration
			config = new PropertiesConfiguration();
			config.setFileName(configFile);
		}

		return config;
	}

	@Override
	protected void configure() {

		// HTTP transport
		bind(HttpTransport.class).to(NetHttpTransport.class).in(Singleton.class);

		// JSON factory
		bind(JsonFactory.class).to(JacksonFactory.class).in(Singleton.class);

		// Drive SDO
		bind(DriveSdo.class).to(DriveSdoImpl.class);

		// File upload Progress Listener
		bind(MediaHttpUploaderProgressListener.class).to(FileUploadProgressListener.class);

		// File download Progress Listener
		bind(MediaHttpDownloaderProgressListener.class).to(FileDownloadProgressListener.class);

		// File Storage Workflow Manager
		bind(FileStorageWorkflowManager.class).to(FileStorageWorkflowManagerImpl.class);

		// Directory Compressor Workflow Manager
		bind(DirectoryCompressorWorkflowManager.class).to(DirectoryCompressorWorkflowManagerImpl.class);

		// Token Workflow Manager
		bind(TokenWorkflowManager.class).to(TokenWorkflowManagerImpl.class);
	}
}
