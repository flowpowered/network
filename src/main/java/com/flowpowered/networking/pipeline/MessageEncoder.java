/*
 * This file is part of Flow Networking, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Spout LLC <http://www.spout.org/>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.flowpowered.networking.pipeline;

import com.flowpowered.networking.Codec;
import com.flowpowered.networking.Message;
import com.flowpowered.networking.process.ProcessingEncoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.List;

import com.flowpowered.networking.protocol.Protocol;

/**
 * A {@link MessageToMessageEncoder} which encodes into {@link ByteBuf}s.
 */
public class MessageEncoder extends ProcessingEncoder {
    private final MessageHandler messageHandler;

    public MessageEncoder(final MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void encodePreProcess(ChannelHandlerContext ctx, final Message message, List<ByteBuf> out) throws IOException {
        final Protocol protocol = messageHandler.getSession().getProtocol();
        final Class<? extends Message> clazz = message.getClass();
        Codec.CodecRegistration reg = protocol.getCodecRegistration(message.getClass());
        if (reg == null) {
            throw new IOException("Unknown message type: " + clazz + ".");
        }
        final ByteBuf messageBuf = reg.getCodec().encode(ctx.alloc().buffer(), message);
        final ByteBuf headerBuf = protocol.writeHeader(reg, messageBuf, ctx.alloc().buffer());
        out.add(Unpooled.wrappedBuffer(headerBuf, messageBuf));
    }
}
