/*
 * This file is part of Flow Network, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2013 Flow Powered <https://flowpowered.com/>
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
package com.flowpowered.network.pipeline;

import java.util.List;

import com.flowpowered.network.processor.MessageProcessor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageProcessorDecoder extends ByteToMessageDecoder {
    private final MessageHandler messageHandler;

    public MessageProcessorDecoder(final MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> frames) throws Exception {
        MessageProcessor processor = getProcessor();
        if (processor == null) {
            frames.add(buf.readBytes(buf.readableBytes()));
            return;
        }
        // Eventually, we will run out of bytes and a ReplayableError will be called
        ByteBuf liveBuffer = ctx.alloc().buffer();
        liveBuffer = processor.processInbound(ctx, buf, liveBuffer);
        frames.add(liveBuffer);
    }

    protected MessageProcessor getProcessor() {
        return messageHandler.getSession().getProcessor();
    }
}
