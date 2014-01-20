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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import com.flowpowered.networking.Codec;
import com.flowpowered.networking.Message;
import com.flowpowered.networking.processor.PreprocessReplayingDecoder;
import com.flowpowered.networking.exception.UnknownPacketException;
import com.flowpowered.networking.processor.MessageProcessor;
import com.flowpowered.networking.protocol.Protocol;

/**
 * A {@link PreprocessReplayingDecoder} which decodes {@link ByteBuf}s into Common {@link Message}s.
 */
public class MessageDecoder extends PreprocessReplayingDecoder {
    private final MessageHandler messageHandler;

    public MessageDecoder(final MessageHandler handler) {
        super(512);
        this.messageHandler = handler;
    }

    @Override
    protected Object decodeProcessed(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        Protocol protocol = messageHandler.getSession().getProtocol();
        Codec<?> codec = null;
        try {
            codec = protocol.readHeader(buf);
        } catch (UnknownPacketException e) {
            // We want to catch this and read the length if possible
            int length = e.getLength();
            if (length != -1 && length != 0) {
                buf.readBytes(length);
            }
            throw e;
        }

        if (codec == null) {
            return buf;
        }

        Message decoded = codec.decode(buf);
        buf.release();
        return decoded;
    }

    @Override
    protected MessageProcessor getProcessor() {
        return messageHandler.getSession().getProcessor();
    }
}
