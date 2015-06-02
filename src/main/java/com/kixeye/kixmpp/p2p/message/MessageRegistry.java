package com.kixeye.kixmpp.p2p.message;

/*
 * #%L
 * Zaqar
 * %%
 * Copyright (C) 2014 Charles Barry
 * %%
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
 * #L%
 */


import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class MessageRegistry {
    public final static int CUSTOM_START_ID = 128;

    private final Map<Class<?>,Integer> classToId = new HashMap<>();
    private final Map<Integer,Class<?>> idToClass = new HashMap<>();

    public MessageRegistry() {
        addMessage( 1, PingRequest.class );
        addMessage( 2, PingResponse.class );
        addMessage( 3, JoinRequest.class );
        addMessage( 4, JoinResponse.class );
    }

    public Class<?> getClassFromId(int idx)  {
        Class<?> clazz = idToClass.get(idx);
        if (clazz != null) {
            return clazz;
        } else {
            throw new NoSuchElementException("idx = " + idx);
        }
    }

    public int getIdFromClass(Class<?> clazz)  {
        Integer idx = classToId.get(clazz);
        if (idx != null) {
            return idx;
        } else {
            throw new NoSuchElementException("class = " + clazz.toString());
       }
    }

    public void addCustomMessage(int id, Class<?> clazz) {
        addMessage(CUSTOM_START_ID + id, clazz);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Internals
    ///////////////////////////////////////////////////////////////////////////

    private synchronized void addMessage(int id, Class<?> clazz) {
        if (idToClass.containsKey(id)) {
            throw new IllegalStateException("id already used: " + id);
        }
        if (classToId.containsKey(clazz)) {
            throw new IllegalStateException("clazz already bound: " + clazz.toString());
        }
        classToId.put(clazz,id);
        idToClass.put(id,clazz);
    }
}
