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

import com.zebrunner.carina.utils.exception.MissingParameterException;

/**
 * DefaultEnvArgsResolver
 * 
 * @deprecated use {@link com.zebrunner.carina.utils.config.Configuration} class instead
 * 
 * @author Aliaksei_Khursevich
 *         <a href="mailto:hursevich@gmail.com">Aliaksei_Khursevich</a>
 *
 */
@Deprecated(forRemoval = true, since = "1.0.5")
public class DefaultEnvArgResolver implements IEnvArgResolver {

    @Override
    public String get(String env, String key) {
        if (Configuration.isNull(Configuration.Parameter.ENV)) {
            throw new MissingParameterException("Configuration parameter 'env' should be set!");
        }
        return R.CONFIG.get(Configuration.get(Configuration.Parameter.ENV) + "." + key);
    }
}
