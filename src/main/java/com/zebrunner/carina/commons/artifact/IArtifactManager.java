package com.zebrunner.carina.commons.artifact;

import java.io.FileNotFoundException;
import java.nio.file.Path;

/**
 * Defines a set of methods for interacting with artifact sources
 */
public interface IArtifactManager {

    /**
     * Download artifact
     *
     * @param from link to the artifact
     * @param to path to save file (indicating the file itself)
     * @return true if download was successful, false otherwise
     * @throws UnsupportedOperationException if the method is not supported by the implementation
     */
    boolean download(String from, Path to);

    /**
     * Put artifact
     *
     * @param from path to the local file
     * @param to where to send the artifact
     * @return true if put was successful, false otherwise
     * @throws UnsupportedOperationException if the method is not supported by the implementation
     */
    boolean put(Path from, String to) throws FileNotFoundException;

    /**
     * Delete artifact
     *
     * @param url link to the file being deleted
     * @return true if deleting was successful, false otherwise
     * @throws UnsupportedOperationException if the method is not supported by the implementation
     */
    boolean delete(String url);

    /**
     * Get direct (pre-signed) link to the artifact<br>
     * Received reference may be identical to passed (depends on implementation)
     */
    String getDirectLink(String url);
}
