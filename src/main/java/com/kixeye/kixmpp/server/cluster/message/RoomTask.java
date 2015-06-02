package com.kixeye.kixmpp.server.cluster.message;

/*
 * #%L
 * KIXMPP
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
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

import com.kixeye.kixmpp.server.module.muc.MucRoom;


public abstract class RoomTask extends ClusterTask {

    private transient MucRoom room;

    private String serviceSubDomain;
    private String roomId;

    public RoomTask() {
    }

    protected RoomTask(MucRoom room, String serviceSubDomain, String roomId) {
        this.room = room;
        this.serviceSubDomain = serviceSubDomain;
        this.roomId = roomId;
    }

    public void setRoom(MucRoom room) {
        this.room = room;
    }

    public MucRoom getRoom() {
        return room;
    }

    public String getServiceSubDomain() {
        return serviceSubDomain;
    }

    public String getRoomId() {
        return roomId;
    }
}
