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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.zebrunner.carina.utils.android.recorder.exception.UnsupportedPlatformException;

public final class Platform {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String NAME = System.getProperty("os.name").toLowerCase(Locale.US);
    public static final boolean IS_WINDOWS = NAME.startsWith("windows");
    public static final boolean IS_MAC_OS_X = NAME.startsWith("mac os x");
    public static final boolean IS_LINUX = NAME.startsWith("linux");

    private static final String[] WIN_CMD = {};
    private static final String[] MAC_CMD = {};

    private Platform() {
    }

    public static String[] getCmd() {
        if (Platform.IS_WINDOWS) {
            return WIN_CMD;
        } else if (Platform.IS_MAC_OS_X || Platform.IS_LINUX) {
            return MAC_CMD;
        }
        throw new UnsupportedPlatformException("Unsupported platform detected.");
    }

    public static int getPID(Process process) {
        if (IS_MAC_OS_X || IS_LINUX) {
            return getUnixProcessPID(process);
        } else if (IS_WINDOWS) {
            return getWinPid(process);
        }
        throw new RuntimeException("Can't get PID properly.");
    }

    public static void killProcesses(Collection<Integer> pids) {
        if (IS_MAC_OS_X || IS_LINUX) {
            killUnixProcessesTree(pids);
        } else if (IS_WINDOWS) {
            killWindowsProcessesTree(pids);
        } else {
            throw new UnsupportedPlatformException("Unsupported platform detected.");
        }
    }

    private static void killWindowsProcessesTree(Collection<Integer> pids) {
        try {
            StringBuilder sb = new StringBuilder("taskkill");
            for (Integer pid : pids) {
                sb.append(" /pid ").append(pid);
            }
            String cmd = sb.append(" /F /T").toString();
            LOGGER.debug(cmd);
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            // do nothing
        }
    }

    private static void killUnixProcessesTree(Collection<Integer> pids) {
        try {
            StringBuilder sb = new StringBuilder("kill -9");
            for (Integer pid : pids) {
                sb.append(" ").append(pid);
            }
            String cmd = sb.toString();
            LOGGER.debug(cmd);
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            // ignore
        }
    }

    private static int getUnixProcessPID(Process process) {
        String processClass = process.getClass().getName();
        if ("java.lang.UNIXProcess".equals(processClass)) {
            try {
                Field f = process.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                return f.getInt(process);
            } catch (Exception e) {
                // do nothing
            }
        }
        return -1;
    }

    private static int getWinPid(Process process) {
        int pid = -1;
        String processClass = process.getClass().getName();

        if ("java.lang.Win32Process".equals(processClass) || "java.lang.ProcessImpl".equals(processClass)) {
            try {
                Field f = process.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handlePtr = f.getLong(process);

                Kernel32 kernel = Kernel32.INSTANCE;
                W32API.HANDLE handle = new W32API.HANDLE();
                handle.setPointer(Pointer.createConstant(handlePtr));
                pid = kernel.GetProcessId(handle);
                return pid;
            } catch (Throwable e) {
                // do nothing
            }
        }
        return -1;
    }

}
