/*
 * Copyright 2012-2016 Luca Zanconato
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

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.nharyes.drivecopy.biz.bo.DirectoryBO;
import net.nharyes.drivecopy.biz.bo.EntryBO;
import net.nharyes.drivecopy.biz.bo.FileBO;
import net.nharyes.drivecopy.biz.bo.TokenBO;
import net.nharyes.drivecopy.biz.exc.WorkflowManagerException;
import net.nharyes.drivecopy.srvc.DriveSdo;
import net.nharyes.drivecopy.srvc.exc.ItemNotFoundException;
import net.nharyes.drivecopy.srvc.exc.SdoException;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

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

    private String[] extractFolders(String filePath) {

        // extract folders
        String[] folders = filePath.split("/");
        if (folders.length > 1) {

            // remove file name from folders
            String[] nf = new String[folders.length - 1];
            System.arraycopy(folders, 0, nf, 0, nf.length);
            folders = nf;

        } else
            folders = null;

        return folders;
    }

    private String extractFileName(String filePath) {

        // return only file name
        if (filePath.contains("/"))
            return filePath.substring(filePath.lastIndexOf("/") + 1);

        return filePath;
    }

    private void checkMD5(EntryBO entry) throws IOException, WorkflowManagerException {

        // calculate MD5 of the local file/directory
        logger.finer("calculate the MD5 summary of the file...");
        byte[] digest = Files.hash(entry.getFile(), Hashing.md5()).asBytes();
        String sDigest = new BigInteger(1, digest).toString(16);
        logger.finer(String.format("digest of the file: %s", sDigest));
        logger.finer(String.format("digest of the entry: %s", entry.getMd5Sum()));

        // compare digests
        if (!sDigest.equalsIgnoreCase(entry.getMd5Sum()))
            throw new WorkflowManagerException("wrong digest!");
        logger.fine("Digests comparison OK");
    }

    private FileBO upsert(FileBO file, boolean upload) throws WorkflowManagerException {

        try {

            // get token
            TokenBO token = getToken();

            // log action
            if (upload)
                logger.info(String.format("Upload '%s' to entry '%s'", file.getFile().getAbsolutePath(), file.getName()));
            else
                logger.info(String.format("Replace entry '%s' with '%s'", file.getName(), file.getFile().getAbsolutePath()));

            // check force option
            if (upload && file.isForce())
                logger.warning("force option ignored");

            EntryBO entry = null;
            String parentId = DriveSdo.DRIVE_ROOT_FOLDER_ID;
            try {

                // process folders and get parent ID
                parentId = driveSdo.getLastFolderId(token, extractFolders(file.getName()), DriveSdo.DRIVE_ROOT_FOLDER_ID, file.isCreateFolders());

                // search entry
                entry = driveSdo.searchEntry(token, extractFileName(file.getName()), parentId);

                if (upload) {

                    // entry already exists
                    throw new SdoException(String.format("Entry with name '%s' already exists", file.getName()));
                }

            } catch (ItemNotFoundException ex) {

                // check force option
                if (!upload && !file.isForce()) {

                    // re-throw exception
                    throw ex;

                } else if (!upload && file.isForce()) {

                    // switch to upload mode
                    upload = true;
                    logger.fine("Switched to upload mode");
                }

                if (upload) {

                    // compose BO
                    entry = new EntryBO();
                    entry.setName(extractFileName(file.getName()));
                }
            }

            // set file property
            assert entry != null;
            entry.setFile(file.getFile());

            // set MIME type property
            entry.setMimeType(file.getMimeType());

            // check skip revision option
            if (file.isSkipRevision() && upload)
                logger.warning("skip revision option ignored");
            else
                entry.setSkipRevision(file.isSkipRevision());

            // check directory
            DirectoryBO dirBO = new DirectoryBO();
            if (file.isDirectory()) {

                // compress directory
                logger.fine(String.format("Compress directory with level '%d'", file.getCompressionLevel()));
                dirBO.setFile(file.getFile());
                dirBO.setLevel(file.getCompressionLevel());
                dirBO = directoryCompressorWorkflowManager.handleWorkflow(dirBO, DirectoryCompressorWorkflowManager.ACTION_COMPRESS);

                // replace file
                entry.setFile(dirBO.getFile());

                // in case set ZIP MIME type
                if (entry.getMimeType() == null)
                    entry.setMimeType("application/zip");

            } else {

                // in case set generic MIME type
                if (entry.getMimeType() == null)
                    entry.setMimeType("application/octet-stream");
            }

            // in case check existing file MD5 summary
            boolean proceedWithReplacement = true;
            if (!upload && file.isArchive()) {

                try {

                    checkMD5(entry);
                    proceedWithReplacement = false;
                    logger.fine("The remote entry already has the same content of the local file.");

                } catch (WorkflowManagerException | IOException ex) {

                    /* wrong digest ignored: the file will be downloaded */
                }
            }

            // upload/replace entry
            if (upload || proceedWithReplacement) {

                logger.finer(String.format("MIME type of the entry: %s", entry.getMimeType()));
                if (upload)
                    entry = driveSdo.uploadEntry(token, entry, parentId);
                else
                    entry = driveSdo.updateEntry(token, entry);

                // check MD5 of the replaced entry
                checkMD5(entry);
            }

            // in case delete temporary file
            if (file.isDirectory()) {

                logger.finer("Delete temporary file");
                if (!entry.getFile().delete())
                    logger.finer("Unable to delete temporary file...");
            }

            // in case delete file or directory
            if (file.isDeleteAfter()) {

                logger.fine("Process file(s) for deletion...");
                processFileForDeletion(file.getFile(), dirBO.getNotCompressed());
            }

            // return updated entry
            FileBO fBO = new FileBO();
            fBO.setFile(entry.getFile());
            fBO.setName(entry.getName());
            return fBO;

        } catch (SdoException | IOException ex) {

            // re-throw exception
            throw new WorkflowManagerException(ex.getMessage(), ex);
        }
    }

    private FileBO upload(FileBO file) throws WorkflowManagerException {

        return upsert(file, true);
    }

    private FileBO download(FileBO file) throws WorkflowManagerException {

        try {

            // get token
            TokenBO token = getToken();

            // log action
            logger.info(String.format("Download entry '%s' to '%s'", file.getName(), file.getFile().getAbsolutePath()));

            // check delete after option
            if (file.isDeleteAfter())
                logger.warning("Delete option ignored");

            // check MIME type option
            if (file.getMimeType() != null)
                logger.warning("MIME type option ignored");

            // check skip revision option
            if (file.isSkipRevision())
                logger.warning("Skip revision option ignored");

            // check create folders option
            if (file.isCreateFolders())
                logger.warning("Create folders option ignored");

            // process folders and get parent ID
            String parentId = driveSdo.getLastFolderId(token, extractFolders(file.getName()), DriveSdo.DRIVE_ROOT_FOLDER_ID, false);

            // search entry
            EntryBO entry = driveSdo.searchEntry(token, extractFileName(file.getName()), parentId);

            // check directory
            boolean downloadFile = true;
            if (file.isDirectory()) {

                // create temporary file
                File tempFile = File.createTempFile("drivecopy" + System.currentTimeMillis(), "temp");
                logger.finer(String.format("Created temporary file '%s'", tempFile.getAbsolutePath()));

                // set file property
                entry.setFile(tempFile);

            } else {

                // set file property
                entry.setFile(file.getFile());

                // in case check existing file MD5 summary
                if (file.isArchive() && entry.getFile().exists()) {

                    try {

                        checkMD5(entry);
                        downloadFile = false;
                        logger.fine("The local file already has the same content of the remote entry.");

                    } catch (WorkflowManagerException | IOException ex) {

                        /* wrong digest ignored: the file will be downloaded */
                    }
                }
            }

            if (downloadFile) {

                // download entry
                entry = driveSdo.downloadEntry(token, entry);

                // check MD5 of the downloaded entry
                checkMD5(entry);
            }

            // check directory
            if (file.isDirectory()) {

                logger.fine("Decompress file");

                // decompress file
                DirectoryBO dirBO = new DirectoryBO();
                dirBO.setFile(entry.getFile());
                dirBO.setDestinationDirectory(file.getFile());
                dirBO = directoryCompressorWorkflowManager.handleWorkflow(dirBO, DirectoryCompressorWorkflowManager.ACTION_DECOMPRESS);

                // delete downloaded file
                logger.finer("Delete downloaded file");
                if (!entry.getFile().delete())
                    logger.finer("Unable to delete downloaded file...");

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

        } catch (SdoException | IOException ex) {

            // re-throw exception
            throw new WorkflowManagerException(ex.getMessage(), ex);
        }
    }

    private FileBO replace(FileBO file) throws WorkflowManagerException {

        return upsert(file, false);
    }

    private void processFileForDeletion(File f, List<File> notCompressed) {

        // check if file not compressed or cannot be read or written
        if (notCompressed.contains(f) || !f.canRead() || !f.canWrite()) {

            // notify UI
            logger.warning(String.format("File '%s' not deleted", f.getAbsolutePath()));

            return;
        }

        // check if file is directory
        if (f.isDirectory()) {

            // process all files contained
            logger.finer(String.format("Process directory '%s' for deletion", f.getAbsolutePath()));
            File[] files = f.listFiles();
            assert files != null;
            for (File fl : files)
                processFileForDeletion(fl, notCompressed);
        }

        // delete file/directory
        logger.finer(String.format("Delete file/directory '%s'", f.getAbsolutePath()));
        if (!f.delete())
            logger.finer("Unable to delete file/directory...");
    }
}
