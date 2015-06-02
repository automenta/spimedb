package com.kixeye.kixmpp.server.module.muc;

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

import java.util.List;

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.server.KixmppServer;

/**
 * A service that handles MUCs.
 * 
 * @author ebahtijaragic
 */
public interface MucService {

    /**
     * Get all the {@link MucRoom}s.
     * @return
     */
    public List<MucRoom> getRooms();

    /**
     * Broadcast the given messages to all rooms {@link MucRoom}.
     * @param jid
     * @param messages
     */
    public void broadcast(KixmppJid jid, String...messages);

    /**
     * Adds a {@link MucRoom}.
     *
     * @param name
     * @return
     */
    public MucRoom addRoom(String name);

    /**
     * Adds a {@link MucRoom}.
     *
     * @param name
     * @param options
     * @return
     */
    public MucRoom addRoom(String name, MucRoomSettings options);

    /**
     * Adds a {@link MucRoom}.
     *
     * @param name
     * @param options
     * @param owner
     * @param ownerNickname
     * @return
     */
    public MucRoom addRoom(String name, MucRoomSettings options, KixmppJid owner, String ownerNickname);


    /**
     * Removes a {@link MucRoom}.
     * @param name
     */
    public void removeRoom(String name);
	/**
	 * Gets a {@link MucRoom}.
	 * 
	 * @param name
	 * @return
	 */
	public MucRoom getRoom(String name);
	
	/**
	 * Returns the server that owns this service.
	 * @return
	 */
	public KixmppServer getServer();
	
	/**
	 * Returns the subdomain for this service.
	 * 
	 * @return
	 */
	public String getSubDomain();
}
