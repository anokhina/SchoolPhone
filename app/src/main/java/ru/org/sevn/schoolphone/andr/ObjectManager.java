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

package ru.org.sevn.schoolphone.andr;

import java.util.HashMap;

public class ObjectManager {
    private ObjectManager() {}

    private static class SingletonHelper {
        private static final ObjectManager instance = new ObjectManager();
    }

    public static ObjectManager getInstance() {
        return SingletonHelper.instance;
    }

    private final HashMap<Long, Object> map = new HashMap<>();
    private long nextId = 0;

    public synchronized long put(Object dr) {
        long ret = nextId++;

        map.put(ret, dr);
        return ret;
    }
    public Object get(long l) {
        return map.get(l);
    }
    // TODO use removed id again
    public void remove(long l) {
        map.remove(l);
    }
    public int size() {
        return map.size();
    }
}
