package com.zebrunner.carina.commons.artifact;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the classes that will be used by Carina Framework to work with various sources of artifacts.
 * The classes over which this annotation is specified must implement the {@link IArtifactManagerFactory} interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ArtifactManagerFactory {
}
