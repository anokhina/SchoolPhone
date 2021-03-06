/*
 * Copyright 2018 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.org.sevn.schoolphone.events;

public class Event {
    private int weekday; // 0 - 6, -1 - none
    private int timeFromHH; // 0 - 23
    private int timeFromMM; // 0 - 59
    private int timeToHH; // 0 - 23
    private int timeToMM; // 0 - 59
    private String name;
    private long alarmBefore; // -1 - none
    private int year;
    private int month;
    private int day;
    private String profile;
}
