/*
 * Copyright 2023 Yury Kharchenko
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

package com.j_phone.system;

/** @noinspection unused*/
public interface MailListener {
 int SKYMAIL = 1;
 int RELAY = 2;
 int GREETING = 3;
 int LONGMAIL = 4;
 int WEB = 5;
 int CBS_DEFINE = 6;
 int CBS_PL = 7;

 void received(String a, String b, int c);
}
