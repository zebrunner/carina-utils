package com.zebrunner.carina.utils.config;

import com.zebrunner.carina.utils.R;

public interface IEnvArgResolver {

    default String get(String env, String key) {
        return R.CONFIG.get(env + "." + key);
    }
}
