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

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.server.module.muc.MucKixmppServerModule;
import com.kixeye.kixmpp.server.module.muc.MucRoom;
import com.kixeye.kixmpp.server.module.muc.MucService;
import io.netty.util.concurrent.Promise;

import java.util.*;

public class GetMucRoomNicknamesRequest extends MapReduceRequest {

	private String gameId;
	private String roomId;

	private transient Promise<Set<String>> promise;
	private transient Set<String> nicknames = Collections.synchronizedSet(new HashSet<String>());

	public GetMucRoomNicknamesRequest() {
	}

	public GetMucRoomNicknamesRequest(String gameId, String roomId, KixmppJid jid, Promise<Set<String>> promise) {
		super(jid);
		this.promise = promise;
		this.gameId = gameId;
		this.roomId = roomId;
	}

	@Override
	public void mergeResponse(MapReduceResponse response) {
		if (response instanceof GetMucRoomNicknamesResponse) {
			GetMucRoomNicknamesResponse msg = (GetMucRoomNicknamesResponse)response;
			if (msg.getUsers() != null) {
				nicknames.addAll(msg.getUsers());
			}
		}
	}

	@Override
	public void onComplete(boolean timedOut) {
		promise.setSuccess(nicknames);
	}

	@Override
	public void run() {
		final MucService mucService = getKixmppServer().module(MucKixmppServerModule.class).getService(gameId);
		if (mucService == null) {
			reply(new GetMucRoomNicknamesResponse(null));
			return;
		}
		MucRoom room = mucService.getRoom(roomId);
		if (room == null) {
			reply(new GetMucRoomNicknamesResponse(null));
			return;
		}
		Set<String> nicknames = new HashSet<>();
		for (MucRoom.User user: room.getUsers()) {
			nicknames.add(user.getNickname());
		}
		reply(new GetMucRoomNicknamesResponse(nicknames));
	}
}
