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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import com.flowpowered.network.processor.MessageProcessor;

/**
 * This class provides a layer of processing after encode but before the message is passed outbound.
 */
public class MessageProcessorEncoder extends MessageToMessageEncoder<ByteBuf> {
    private final MessageHandler messageHandler;

    public MessageProcessorEncoder(final MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, final ByteBuf buf, List<Object> out) throws Exception {
        final MessageProcessor processor = getProcessor();
        if (processor == null) {
            out.add(buf.readBytes(buf.readableBytes()));
            return;
        }
        ByteBuf toAdd = ctx.alloc().buffer();
        toAdd = processor.processOutbound(ctx, buf, toAdd);
        out.add(toAdd);
    }

    protected MessageProcessor getProcessor() {
        return messageHandler.getSession().getProcessor();
    }
}
