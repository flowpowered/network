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

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import com.flowpowered.networking.Codec;
import com.flowpowered.networking.Message;
import com.flowpowered.networking.process.PreprocessReplayingDecoder;
import com.flowpowered.networking.exception.UnknownPacketException;
import com.flowpowered.networking.protocol.Protocol;

/**
 * A {@link PreprocessReplayingDecoder} which decodes {@link ByteBuf}s into Common {@link Message}s.
 */
public class MessageDecoder extends PreprocessReplayingDecoder {
    private static final int PREVIOUS_MASK = 0x1F;
    private final MessageHandler messageHandler;
    private int[] previousOpcodes = new int[PREVIOUS_MASK + 1];
    private int opcodeCounter = 0;

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
            int length = e.getLength();
            if (length != -1 && length != 0) {
                buf.readBytes(length);
            }
            if (length == -1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < PREVIOUS_MASK; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(Integer.toHexString(previousOpcodes[(opcodeCounter + i) & PREVIOUS_MASK]));
                }

                throw new IOException("Unknown operation code: " + e.getOpcode() + " (previous opcodes: " + sb.toString() + ").", e);
            }
        }

        if (codec == null) {
            return buf;
        }

        previousOpcodes[(opcodeCounter++) & PREVIOUS_MASK] = codec.getOpcode();
        Message decoded = codec.decode(buf);
        buf.release();
        return decoded;
    }
}
