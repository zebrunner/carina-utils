/*******************************************************************************
 * Copyright 2020-2022 Zebrunner Inc (https://www.zebrunner.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.zebrunner.carina.utils.ftp;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Base64;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated not used
 */
@Deprecated(forRemoval = true, since = "1.0.5")
public final class FtpUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final int DEFAULT_PORT = 21;
	private static int uploading = 0;

    private FtpUtils() {
    }

	public static void uploadFile(String ftpHost, String user, String password, String filePassToUpload,
			String fileName) {
		uploadFile(ftpHost, DEFAULT_PORT, user, password, filePassToUpload, fileName);
	}

	public static void uploadFile(String ftpHost, int port, String user, String password, String filePassToUpload,
			String fileName) {
		try (InputStream is = new FileInputStream(filePassToUpload)) {
			upload(ftpHost, port, user, password, is, fileName);
		} catch (FileNotFoundException e) {
			LOGGER.info("File is not found. Specify correct file pass");
		} catch (IOException e) {
			LOGGER.info("Exception while opening file for upload.");
		}
	}

	public static void uploadData(String ftpHost, String user, String password, String data,
			String destinationFileName) {
		uploadData(ftpHost, DEFAULT_PORT, user, password, data, destinationFileName);
	}

	public static void uploadData(String ftpHost, int port, String user, String password, String data,
			String destinationFileName) {
		byte[] decode = Base64.getDecoder().decode(data);
        LOGGER.debug("Data size to upload: {}", data.length());
        LOGGER.debug("Encoded data size to upload: {}", decode.length);
		try (InputStream is = new ByteArrayInputStream(decode)) {
			upload(ftpHost, port, user, password, is, destinationFileName);
		} catch (IOException e) {
			LOGGER.info("Exception while opening file for upload.");
		}
	}

	private static void upload(String ftpHost, int port, String user, String password, InputStream is,
			String fileName) {
        LOGGER.debug("FTP host to upload data : {}", ftpHost);
        LOGGER.debug("FTP port to upload data : {}", port);
        LOGGER.debug("Destination file name : {}", fileName);
        long start = System.currentTimeMillis();
		FTPClient ftp = new FTPClient();
		try {
			int reply;
			ftp.connect(ftpHost, port);
            LOGGER.debug("Connected to server : {}", ftpHost);
			reply = ftp.getReplyCode();
            LOGGER.debug("Reply code is : {}", reply);
			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
                LOGGER.error("FTP server refused connection. Reply code is : {}", reply);
				throw new Exception("FTP server refused connection.");
			}
			if (!ftp.login(user, password)) {
			    throw new Exception("Login to ftp failed. Check user credentials.");
			}
			LOGGER.debug("User has been successfully logged in.");
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			try {
			    ftp.enterLocalPassiveMode();
                LOGGER.debug("Passive host : {} Passive port : {}", ftp.getPassiveHost(), ftp.getPassivePort());
			    LOGGER.debug("Default port : {}", ftp.getDefaultPort());
			    LOGGER.debug("Local port : {}", ftp.getLocalPort());
			    LOGGER.debug("Remote port : {}", ftp.getRemotePort());
			    
			    uploading++;
			    LOGGER.info("Uploading video: {}", fileName);
				if (ftp.storeFile(fileName, is)) {
                    LOGGER.info("Uploaded video in {} msecs for: {}",  System.currentTimeMillis() - start, fileName);
				} else {
				    LOGGER.error("Failed to upload video in {} msecs for: {}",System.currentTimeMillis() - start, fileName);
				}
			} catch (IOException e) {
				LOGGER.error("Exception while storing file to FTP", e);
			} finally {
			    uploading--;
			}
		} catch (Exception e) {
			LOGGER.error("Exception while uploading while to FTP", e);
		} finally {
			ftpDisconnect(ftp);
		}
	}

    public static void ftpDisconnect(FTPClient ftp) {
        try {
            if (ftp.isConnected()) {
                ftp.logout();
                ftp.disconnect();
                LOGGER.debug("FTP has been successfully disconnected.");
            }
        } catch (Exception ioe) {
            LOGGER.error("Exception while disconnecting ftp", ioe);
        }
    }

    public static boolean isUploading() {
        return uploading > 0;
    }
}
