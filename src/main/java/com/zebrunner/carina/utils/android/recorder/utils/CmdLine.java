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
package com.zebrunner.carina.utils.android.recorder.utils;

import java.util.List;

/**
 * Created by YP.
 * Date: 8/19/2014
 * Time: 12:52 AM
 */
public final class CmdLine {

    private CmdLine() {
    }

    public static String[] createPlatformDependentCommandLine(String... command) {
        return mergeCommands(Platform.getCmd(), command);
    }

    public static String[] insertCommandsAfter(String[] originalCmd, String... extraCommands) {
        return mergeCommands(originalCmd, extraCommands);
    }

    public static String[] insertCommandsBefore(String[] originalCmd, String... extraCommands) {
        return mergeCommands(extraCommands, originalCmd);
    }

    public static String[] mergeCommands(String[] cmd1, String[] cmd2) {
        int newArraySize = cmd1.length + cmd2.length;
        String[] newCommands = new String[newArraySize];
        int i = 0;
        for (String cmd : cmd1) {
            newCommands[i++] = cmd;
        }
        for (String cmd : cmd2) {
            newCommands[i++] = cmd;
        }
        return newCommands;
    }

    public static String arrayToString(String[] params) {
        StringBuilder b = new StringBuilder();
        for (String s : params) {
            b.append(s);
            b.append(" ");
        }
        return b.toString();
    }

    public static String listToString(List<String> params) {
        StringBuilder b = new StringBuilder();
        for (String s : params) {
            b.append(s);
            b.append(" ");
        }
        return b.toString();
    }

}