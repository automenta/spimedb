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

/**
 * Settings for {@link MucRoom}
 *
 * @author dturner@kixeye.com
 */
public class MucRoomSettings {
	private boolean presenceEnabled = false;
    private boolean isOpen = true;
    private String subject = null;
    
    public MucRoomSettings(boolean presenceEnabled, boolean isOpen, String subject) {
	    this.presenceEnabled = presenceEnabled;
		this.isOpen = isOpen;
		this.subject = subject;
	}

    public MucRoomSettings(MucRoomSettings settings) {
	    this.presenceEnabled = settings.presenceEnabled;
    	this.isOpen = settings.isOpen;
    	this.subject = settings.subject;
    }
    
	public MucRoomSettings() {
    }

    public void setOpen(boolean isOpen){
        this.isOpen = isOpen;
    }

    public void setSubject(String subject){
        this.subject = subject;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public String getSubject() {
        return subject;
    }

	public boolean isPresenceEnabled() { return presenceEnabled; }
}
