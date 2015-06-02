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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Attribute;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.kixeye.kixmpp.KixmppJid;
import com.kixeye.kixmpp.handler.KixmppEventEngine;
import com.kixeye.kixmpp.server.KixmppServer;
import com.kixeye.kixmpp.server.module.bind.BindKixmppServerModule;

/**
 * tests for {@link MucRoom}
 *
 * @author dturner@kixeye.com
 */
@SuppressWarnings("unchecked")
public class MucRoomTest {
	@Test
    public void joinRoom_firstTime_isOpen() {
    	KixmppServer server = (KixmppServer)Mockito.when(Mockito.mock(KixmppServer.class).getEventEngine())
    			.thenReturn(new KixmppEventEngine()).getMock();
    	MucService mucService = (MucService)Mockito.when(Mockito.mock(MucService.class).getServer())
    			.thenReturn(server).getMock();
    	
        KixmppJid roomJid = new KixmppJid("testnode", "testdomain");
        MucRoom mucRoom = new MucRoom(mucService, roomJid, new MucRoomSettings(false, true, null));

        Channel channel = Mockito.mock(Channel.class);
        Attribute<KixmppJid> jidAttribute = Mockito.mock(Attribute.class);
        Mockito.when(jidAttribute.get()).thenReturn(new KixmppJid("test.user", "testdomain", "testresource"));
        Mockito.when(channel.attr(BindKixmppServerModule.JID)).thenReturn(jidAttribute);
        Mockito.when(channel.closeFuture()).thenReturn(Mockito.mock(ChannelFuture.class));

        Assert.assertEquals(0, mucRoom.getUsers().size());

        mucRoom.join(channel, "nickname");

        Assert.assertEquals(1, mucRoom.getUsers().size());
    }

    @Test
    public void joinRoom_multipleConnectionsSameUser_isOpen() {
    	KixmppServer server = (KixmppServer)Mockito.when(Mockito.mock(KixmppServer.class).getEventEngine())
    			.thenReturn(new KixmppEventEngine()).getMock();
    	MucService mucService = (MucService)Mockito.when(Mockito.mock(MucService.class).getServer())
    			.thenReturn(server).getMock();
    	
        KixmppJid roomJid = new KixmppJid("testnode", "testdomain");
        
        MucRoom mucRoom = new MucRoom(mucService, roomJid, new MucRoomSettings(false, true, null));

        Channel channel = Mockito.mock(Channel.class);
        Attribute<KixmppJid> jidAttribute = Mockito.mock(Attribute.class);
        Mockito.when(jidAttribute.get()).thenReturn(new KixmppJid("test.user", "testdomain", "testresource"));
        Mockito.when(channel.attr(BindKixmppServerModule.JID)).thenReturn(jidAttribute);
        Mockito.when(channel.closeFuture()).thenReturn(Mockito.mock(ChannelFuture.class));

        Assert.assertEquals(0, mucRoom.getUsers().size());

        mucRoom.join(channel, "nickname");

        Assert.assertEquals(1, mucRoom.getUsers().size());

        Channel channel2 = Mockito.mock(Channel.class);
        Attribute<KixmppJid> jidAttribute2 = Mockito.mock(Attribute.class);
        Mockito.when(jidAttribute2.get()).thenReturn(new KixmppJid("test.user", "testdomain", "testresource2"));
        Mockito.when(channel2.attr(BindKixmppServerModule.JID)).thenReturn(jidAttribute2);
        Mockito.when(channel2.closeFuture()).thenReturn(Mockito.mock(ChannelFuture.class));

        mucRoom.join(channel2, "nickname");

        Assert.assertEquals(1, mucRoom.getUsers().size());
        Assert.assertEquals(2, mucRoom.getUser("nickname").getConnections().size());
    }

    @Test
    public void joinRoom_conflictingNickname_isOpen() {
    	KixmppServer server = (KixmppServer)Mockito.when(Mockito.mock(KixmppServer.class).getEventEngine())
    			.thenReturn(new KixmppEventEngine()).getMock();
    	MucService mucService = (MucService)Mockito.when(Mockito.mock(MucService.class).getServer())
    			.thenReturn(server).getMock();
    	
        KixmppJid roomJid = new KixmppJid("testnode", "testdomain");
        MucRoom mucRoom = new MucRoom(mucService, roomJid, new MucRoomSettings(false, true, null));

        Channel channel = Mockito.mock(Channel.class);
        Attribute<KixmppJid> jidAttribute = Mockito.mock(Attribute.class);
        Mockito.when(jidAttribute.get()).thenReturn(new KixmppJid("test.user1", "testdomain", "testresource"));
        Mockito.when(channel.attr(BindKixmppServerModule.JID)).thenReturn(jidAttribute);
        Mockito.when(channel.closeFuture()).thenReturn(Mockito.mock(ChannelFuture.class));

        Assert.assertEquals(0, mucRoom.getUsers().size());

        mucRoom.join(channel, "nickname");

        Assert.assertEquals(1, mucRoom.getUsers().size());

        Channel channel2 = Mockito.mock(Channel.class);
        Attribute<KixmppJid> jidAttribute2 = Mockito.mock(Attribute.class);
        Mockito.when(jidAttribute2.get()).thenReturn(new KixmppJid("test.user2", "testdomain", "testresource"));
        Mockito.when(channel2.attr(BindKixmppServerModule.JID)).thenReturn(jidAttribute2);
        Mockito.when(channel2.closeFuture()).thenReturn(Mockito.mock(ChannelFuture.class));

        try {
            mucRoom.join(channel2, "nickname");
            Assert.fail();
        } catch (NicknameInUseException e) {

        }
    }

    @Test(expected = RoomJoinNotAllowedException.class)
    public void joinRoom_firstTime_isNotOpen_noMemberAdded() {
    	KixmppServer server = (KixmppServer)Mockito.when(Mockito.mock(KixmppServer.class).getEventEngine())
    			.thenReturn(new KixmppEventEngine()).getMock();
    	MucService mucService = (MucService)Mockito.when(Mockito.mock(MucService.class).getServer())
    			.thenReturn(server).getMock();
    	
        KixmppJid roomJid = new KixmppJid("testnode", "testdomain");
        MucRoom mucRoom = new MucRoom(mucService, roomJid, new MucRoomSettings(false, false, null));

        Channel channel = Mockito.mock(Channel.class);
        Attribute<KixmppJid> jidAttribute = Mockito.mock(Attribute.class);
        Mockito.when(jidAttribute.get()).thenReturn(new KixmppJid("test.user", "testdomain", "testresource"));
        Mockito.when(channel.attr(BindKixmppServerModule.JID)).thenReturn(jidAttribute);
        Mockito.when(channel.closeFuture()).thenReturn(Mockito.mock(ChannelFuture.class));

        mucRoom.join(channel, "nickname");
    }

    @Test
    public void joinRoom_firstTime_isNotOpen_memberAdded() {
    	KixmppServer server = (KixmppServer)Mockito.when(Mockito.mock(KixmppServer.class).getEventEngine())
    			.thenReturn(new KixmppEventEngine()).getMock();
    	MucService mucService = (MucService)Mockito.when(Mockito.mock(MucService.class).getServer())
    			.thenReturn(server).getMock();
    	
        KixmppJid roomJid = new KixmppJid("testnode", "testdomain");
        MucRoom mucRoom = new MucRoom(mucService, roomJid, new MucRoomSettings(false, false, null));

        
        Channel channel = Mockito.mock(Channel.class);
        Attribute<KixmppJid> jidAttribute = Mockito.mock(Attribute.class);
        Mockito.when(jidAttribute.get()).thenReturn(new KixmppJid("test.user", "testdomain", "testresource"));
        Mockito.when(channel.attr(BindKixmppServerModule.JID)).thenReturn(jidAttribute);
        Mockito.when(channel.closeFuture()).thenReturn(Mockito.mock(ChannelFuture.class));

        Assert.assertEquals(0, mucRoom.getUsers().size());

        mucRoom.addUser(new KixmppJid("test.user", "testdomain", "testresource"), "nickname", MucRole.Participant, MucAffiliation.Member);
        mucRoom.join(channel, "nickname");

        Assert.assertEquals(1, mucRoom.getUsers().size());
    }

    @Test
    public void removeUser_userNotInRoom(){
        KixmppJid roomJid = new KixmppJid("testnode", "testdomain");
        MucRoom mucRoom = new MucRoom((MucService)Mockito.when(Mockito.mock(MucService.class).getServer()).thenReturn(Mockito.mock(KixmppServer.class)).getMock(), roomJid, new MucRoomSettings(false, false, null));

        Assert.assertFalse(mucRoom.removeUser(new KixmppJid("test.user","testdomain")));
    }

    @Test
    public void removeUser_userInRoom(){
    	KixmppServer server = (KixmppServer)Mockito.when(Mockito.mock(KixmppServer.class).getEventEngine())
    			.thenReturn(new KixmppEventEngine()).getMock();
    	MucService mucService = (MucService)Mockito.when(Mockito.mock(MucService.class).getServer())
    			.thenReturn(server).getMock();
    	
        KixmppJid roomJid = new KixmppJid("testnode", "testdomain");
        MucRoom mucRoom = new MucRoom(mucService, roomJid, new MucRoomSettings(false, true, null));

        KixmppJid clientJid = new KixmppJid("test.user", "testdomain", "testresource");

        Channel channel = Mockito.mock(Channel.class);
        Attribute<KixmppJid> jidAttribute = Mockito.mock(Attribute.class);
        Mockito.when(jidAttribute.get()).thenReturn(clientJid);
        Mockito.when(channel.attr(BindKixmppServerModule.JID)).thenReturn(jidAttribute);
        Mockito.when(channel.closeFuture()).thenReturn(Mockito.mock(ChannelFuture.class));

        Assert.assertEquals(0, mucRoom.getUsers().size());

        mucRoom.join(channel, "nickname");

        Assert.assertEquals(1, mucRoom.getUsers().size());

        Assert.assertTrue(mucRoom.removeUser(clientJid));

        Assert.assertEquals(0, mucRoom.getUsers().size());
    }

    @Test
    public void removeAndRejoinUser(){
    	KixmppServer server = (KixmppServer)Mockito.when(Mockito.mock(KixmppServer.class).getEventEngine())
    			.thenReturn(new KixmppEventEngine()).getMock();
    	MucService mucService = (MucService)Mockito.when(Mockito.mock(MucService.class).getServer())
    			.thenReturn(server).getMock();
    	
        KixmppJid roomJid = new KixmppJid("testnode", "testdomain");
        MucRoom mucRoom = new MucRoom(mucService, roomJid, new MucRoomSettings(false, false, null));

        KixmppJid clientJid = new KixmppJid("test.user", "testdomain", "testresource");

        Channel channel = Mockito.mock(Channel.class);
        Attribute<KixmppJid> jidAttribute = Mockito.mock(Attribute.class);
        Mockito.when(jidAttribute.get()).thenReturn(clientJid);
        Mockito.when(channel.attr(BindKixmppServerModule.JID)).thenReturn(jidAttribute);
        Mockito.when(channel.closeFuture()).thenReturn(Mockito.mock(ChannelFuture.class));

        Assert.assertEquals(0, mucRoom.getUsers().size());

        try{
            mucRoom.join(channel, "nickname");
            Assert.fail();
        } catch (RoomJoinNotAllowedException e){
            //expected
        }

        Assert.assertEquals(0, mucRoom.getUsers().size());

        mucRoom.addUser(clientJid, "nickname", MucRole.Participant, MucAffiliation.Member);
        mucRoom.join(channel, "nickname");

        Assert.assertEquals(1, mucRoom.getUsers().size());
        Assert.assertTrue(mucRoom.removeUser(clientJid));
        Assert.assertEquals(0, mucRoom.getUsers().size());

        mucRoom.addUser(clientJid, "nickname", MucRole.Participant, MucAffiliation.Member);
        mucRoom.join(channel, "nickname");

        Assert.assertEquals(1, mucRoom.getUsers().size());
    }
}
