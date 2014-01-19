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
package com.flowpowered.networking.process;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import com.flowpowered.networking.Message;

/**
 * This class provides a layer of processing after encode but before the message is passed outbound.
 */
public abstract class ProcessingEncoder extends MessageToMessageEncoder<Message> {
    private final AtomicReference<ChannelProcessor> processor = new AtomicReference<>();

    private void checkForSetupMessage(Object e) {
        if (e instanceof ProcessorSetupMessage) {
            ProcessorSetupMessage setupMessage = (ProcessorSetupMessage) e;
            ChannelProcessor newProcessor = setupMessage.getProcessor();
            processor.set(newProcessor);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, final Message msg, List<Object> out) throws Exception {
        List<ByteBuf> newOut = new ArrayList<>();
        encodePreProcess(ctx, msg, newOut);
        final ChannelProcessor processor = this.processor.get();
        for (final ByteBuf encoded : newOut) {
            ByteBuf toAdd = ctx.alloc().buffer();
            if (processor != null) {
                processor.process(ctx, encoded, toAdd);
                // Gotta release the old
                encoded.release();
            }
            out.add(toAdd);
        }
        checkForSetupMessage(msg);
    }

    protected abstract void encodePreProcess(ChannelHandlerContext ctx, Message msg, List<ByteBuf> out) throws Exception;
}
