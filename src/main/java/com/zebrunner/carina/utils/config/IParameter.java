package com.zebrunner.carina.utils.config;

public interface IParameter {

    String getKey();

    default boolean hidden() {
        return false;
    }
}
