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
package com.zebrunner.carina.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void removeDirRecurs(String directory) {
        File dir = new File(directory);
        if (dir.exists() && dir.isDirectory()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    public static synchronized List<File> getFilesInDir(File directory) {
        List<File> files = new ArrayList<>();
        try {
            File[] fileArray = directory.listFiles();
            if (fileArray == null) {
                LOGGER.debug("'{}' does not denote a directory, or if an I/O error occurs when an attempt was made to get a list of files",
                        directory.getAbsolutePath());
                return files;
            }
            files.addAll(Arrays.asList(fileArray));
        } catch (Exception e) {
            LOGGER.error("Unable to get files in dir!", e);
        }
        return files;
    }

    public static void createFileWithContent(String filePath, String content) {
        File file = new File(filePath);
        try {
            boolean isSuccess = file.createNewFile();
            if (!isSuccess) {
                LOGGER.debug("File '{}' already exists.", file.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.debug("Error during creating new file with path {}.", filePath, e);
            return;
        }

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        } catch (IOException e) {
            LOGGER.debug("Error during writing content to the file.", e);
        }
    }

    /**
     * Archive list of files into the single zip archive.
     *
     * @param output
     *          String zip file path.
     * @param files 
     *          List of files to archive
     */
    public static void zipFiles(String output, File... files) {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(output))) {
            for (File fileToZip : files) {
                zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
                Files.copy(fileToZip.toPath(), zipOut);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Unable to find file for archive operation!", e);
        } catch (IOException e) {
            LOGGER.error("IO exception for archive operation!", e);
        }
    }

    /**
     * Get file checksum.
     *
     * @param checksumType  Checksum hash type.
     * @param file file path.
     * @return hash as a StringF
     * @throws NoSuchAlgorithmException can be caused by read() method
     * @throws IOException can be caused by read() method MessageDigest.getInstance() method
     */
    public static String getFileChecksum(Checksum checksumType, File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(checksumType.value);

        try (FileInputStream fileInputStream = new FileInputStream(file); FileChannel channel = fileInputStream.getChannel()) {
            final ByteBuffer buf = ByteBuffer.allocateDirect(8192);
            int buffer = channel.read(buf);

            while (buffer != -1 && buffer != 0) {
                buf.flip();
                final byte[] bytes = new byte[buffer];
                buf.get(bytes);
                digest.update(bytes, 0, buffer);
                buf.clear();
                buffer = channel.read(buf);
            }

            return Base64.encodeBase64String(digest.digest());
        }
    }

    public enum Checksum {
        MD5("MD5"),
        SHA_256("SHA-256");

        public final String value;

        private Checksum(String value) {
            this.value = value;
        }
    }
}
