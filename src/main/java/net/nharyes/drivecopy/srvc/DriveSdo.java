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

package net.nharyes.drivecopy.srvc;

import net.nharyes.drivecopy.biz.bo.EntryBO;
import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.srvc.exc.SdoException;

public interface DriveSdo {

	EntryBO downloadEntry(TokenBO token, EntryBO entry) throws SdoException;

	EntryBO uploadEntry(TokenBO token, EntryBO entry, String parentId) throws SdoException;

	EntryBO updateEntry(TokenBO token, EntryBO entry) throws SdoException;

	EntryBO searchEntry(TokenBO token, String name, String parentId) throws SdoException;

	String getLastFolderId(TokenBO token, String[] folders, boolean createIfNotFound) throws SdoException;
}
