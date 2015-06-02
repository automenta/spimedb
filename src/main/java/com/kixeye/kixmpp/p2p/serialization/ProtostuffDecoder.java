package com.kixeye.kixmpp.p2p.serialization;

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

import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.kixeye.kixmpp.p2p.message.MessageRegistry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.io.IOException;
import java.util.List;


public class ProtostuffDecoder extends MessageToMessageDecoder<ByteBuf>{

    private final MessageRegistry registry;

    public ProtostuffDecoder(MessageRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        Object obj = deserializeFromByteBuf(registry,msg);
        out.add(obj);
    }


    /**
     * Expose deserializer for unit testing.
     *
     * @param registry
     * @param buf
     * @return
     * @throws IOException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object deserializeFromByteBuf(MessageRegistry registry, ByteBuf buf) throws IOException {
        // get message class
        int typeIdx = buf.readInt();
        Class<?> clazz = registry.getClassFromId(typeIdx);

        // decode rest of array into object
        Schema schema = RuntimeSchema.getSchema(clazz);
        Object obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(new ByteBufInputStream(buf),obj,schema);

        return obj;
    }
}
