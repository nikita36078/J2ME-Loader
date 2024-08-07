/*
 * Copyright 2021-2022 Arman Jussupgaliyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nokia.mid.ui;

import javax.microedition.lcdui.Font;

public class FreeSizeFontInvoker {

    public static void setInvoker(FreeSizeFontInvoker invoker) {
    }

    public static Font getFont(int face, int style, int height) {
        return new Font(face, style, -1, height);
    }
}
