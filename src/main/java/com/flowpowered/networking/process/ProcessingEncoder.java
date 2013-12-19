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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a layer of processing after encode but before the message is passed outbound.
 *
 */
public abstract class ProcessingEncoder extends MessageToMessageEncoder<Object> implements ProcessorHandler {
    private final AtomicReference<ChannelProcessor> processor = new AtomicReference<>();
    private final AtomicBoolean locked = new AtomicBoolean(false);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (locked.get()) {
            throw new IllegalStateException("Encode attempted when channel was locked");
        }
        super.write(ctx, msg, promise);
    }

    private void checkForSetupMessage(Object e) {
        if (e instanceof ProcessorSetupMessage) {
            ProcessorSetupMessage setupMessage = (ProcessorSetupMessage) e;
            ChannelProcessor newProcessor = setupMessage.getProcessor();
            if (newProcessor != null) {
                setProcessor(newProcessor);
            }
            if (setupMessage.isChannelLocking()) {
                locked.set(true);
            } else {
                locked.set(false);
            }
            setupMessage.setProcessorHandler(this);
        }
    }

    @Override
    public void setProcessor(ChannelProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("Processor may not be set to null");
        } else if (!this.processor.compareAndSet(null, processor)) {
            throw new IllegalArgumentException("Processor may only be set once");
        }
        locked.set(false);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, final Object msg, List<Object> out) throws Exception {
        List<Object> newOut = new ArrayList<>();
        encodePreProcess(ctx, msg, newOut);
        final ChannelProcessor processor = this.processor.get();
        for (final Object encoded : newOut) {
            Object toAdd = encoded;
            if (processor != null && encoded instanceof ByteBuf) {
                synchronized (this) {
                    // Gotta release the old
                    toAdd = processor.process(ctx, (ByteBuf) encoded, ctx.alloc().buffer());
                    ((ByteBuf) encoded).release();
                }
            }
            out.add(toAdd);
        }
        checkForSetupMessage(msg);
    }

    protected abstract void encodePreProcess(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception;
}
