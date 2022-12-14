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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZipManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ZipManager() {
    }

    @SuppressWarnings("rawtypes")
    public static void unzip(String zip, String extractTo) {
        try (ZipFile zipFile = new ZipFile(zip)) {
            Enumeration entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    Path path = Path.of(extractTo, entry.getName());
                    File folder = path.toFile();
                    boolean isCreated = folder.mkdir();
                    if (!isCreated) {
                        throw new UncheckedIOException(new IOException("Folder not created: " + folder.getAbsolutePath()));
                    }
                    continue;
                }

                try (InputStream is = zipFile.getInputStream(entry);
                        OutputStream fos = new FileOutputStream(Path.of(extractTo, entry.getName()).toFile());
                        OutputStream bos = new BufferedOutputStream(fos)) {
                    copyInputStream(is, bos);
                }
            }
        } catch (IOException e) {
            LOGGER.error("IO exception for unzip operation!", e);
        }
    }

    public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;

        if (in == null) {
        	return;
        }
        
        while ((len = in.read(buffer)) >= 0)
            out.write(buffer, 0, len);

        in.close();
        out.close();
    }
}
