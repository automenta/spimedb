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

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.kixeye.kixmpp.p2p.message.MessageRegistry;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.io.IOException;
import java.util.List;


public class ProtostuffEncoder extends MessageToMessageEncoder<Object> {

    private final MessageRegistry registry;

    public ProtostuffEncoder(MessageRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
        if (msg instanceof ByteBuf) {
            // already serialized so just pass through
            ByteBuf buf = (ByteBuf) msg;
            out.add(buf.retain());
        } else {
            // serialize
            ByteBuf buf = ctx.alloc().buffer();
            serializeToByteBuf(registry, buf, msg);
            out.add(buf);
        }
    }


    /**
     * Expose serializer for sharing serialization and unit testing.
     *
     * @param registry
     * @param buf
     * @return
     * @throws IOException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static ByteBuf serializeToByteBuf(MessageRegistry registry, ByteBuf buf, Object msg) throws IOException {
        // write class id
        int classIdx = registry.getIdFromClass(msg.getClass());
        buf.writeInt(classIdx);

        // write serialized object
        Schema schema = RuntimeSchema.getSchema(msg.getClass());
        LinkedBuffer linkedBuffer = LinkedBuffer.allocate(1024);
        ProtostuffIOUtil.writeTo(new ByteBufOutputStream(buf), msg, schema, linkedBuffer);

        return buf;
    }
}
