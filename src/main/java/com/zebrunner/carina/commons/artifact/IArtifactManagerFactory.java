package com.zebrunner.carina.commons.artifact;

public interface IArtifactManagerFactory {

    /**
     * Determines whether the current implementation fits the passed artifact reference
     *
     * @param url link to the artifact
     * @return true if it fits, false otherwise
     */
    boolean isSuitable(String url);

    /**
     * Get an instance of the artifact manager
     *
     * @return see {@link IArtifactManager}
     */
    public IArtifactManager getInstance();
}
