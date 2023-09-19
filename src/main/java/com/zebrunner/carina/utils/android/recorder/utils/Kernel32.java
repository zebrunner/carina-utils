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

import com.sun.jna.Native;

interface Kernel32 extends W32API {
    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, Boolean.getBoolean("w32.ascii") ? ASCII_OPTIONS : UNICODE_OPTIONS);

    HANDLE GetCurrentProcess();

    int GetProcessId(HANDLE Process);
}
