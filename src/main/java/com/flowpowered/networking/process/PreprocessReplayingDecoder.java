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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * This class is both a {@link ByteToMessageDecoder} but also allows processing pre-decode via {@code decodeProcessed}.
 *
 */
public abstract class PreprocessReplayingDecoder extends ByteToMessageDecoder implements DecodingProcessorHandler {
    private final AtomicReference<ChannelProcessor> processor = new AtomicReference<>(null);
    private final ReplayableByteBuf replayableBuffer = new ReplayableByteBuf();
    private final int capacity;
    private ByteBuf processedBuffer = null;

    /**
     * Constructs a new replaying decoder.<br> <br> The internal buffer is dynamically sized, but if it grows larger than the given capacity, it will be resized downwards when possible.  This allows
     * handling of larger packets without requiring the buffers to be set larger than the size of the largest packet.
     *
     * @param capacity the default capacity of the internal buffer.
     */
    public PreprocessReplayingDecoder(int capacity) {
        this.capacity = capacity;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> frames) throws Exception {
        Object frame;
        ByteBuf liveBuffer;
        do {
            ChannelProcessor processor = this.processor.get();
            if (processor == null) {
                liveBuffer = buf;
            } else {
                if (processedBuffer == null) {
                    processedBuffer = ctx.alloc().buffer();
                }
                processor.process(ctx, buf, processedBuffer);
                liveBuffer = processedBuffer;
            }
            int readPointer = liveBuffer.readerIndex();
            try {
                frame = decodeProcessed(ctx, replayableBuffer.setBuffer(liveBuffer));
            } catch (ReplayableException e) {
                // roll back liveBuffer read to state prior to calling decodeProcessed
                liveBuffer.readerIndex(readPointer);
                // No frame returned
                frame = null;
            }

            if (frame != null) {
                frames.add(frame);
                if (frame instanceof ProcessorSetupMessage) {
                    ProcessorSetupMessage setupMessage = (ProcessorSetupMessage) frame;
                    setupMessage.setDecodingProcessorHandler(this);
                }
            }
        } while (frame != null);

        // This is to ensure that the processedBuffer doesn't get too large
        // We want to try to cap the processedBuffer at capacity
        if (processedBuffer != null) {
            if (processedBuffer.capacity() > capacity && processedBuffer.isWritable()) {
                ByteBuf newBuffer = ctx.alloc().buffer(Math.max(capacity, processedBuffer.readableBytes()));
                if (processedBuffer.isReadable()) {
                    // This method transfers the data in processedBuffer to the newBuffer.
                    // However, for some reason, if processedBuffer is zero length, it causes an exception; therefore, we check if we have any readable bytes (more than 0)
                    // TODO: test if true
                    newBuffer.writeBytes(processedBuffer);
                }
                ByteBuf old = processedBuffer;
                processedBuffer = newBuffer;
                old.release();
            } else {
                // If the capacity was greater, then we've discarded all the read bytes already
                processedBuffer.discardReadBytes();
            }
        }
    }

    @Override
    public void setProcessor(ChannelProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("Processor may not be set to null");
        }
        this.processor.set(processor);
    }

    /**
     * This method is the equivalent of the decode method for the standard ReplayingDecoder<br> The method call is repeated if decoding causes the ByteBuf to run out of bytes<br>
     *
     * @param ctx the channel handler context
     * @param buffer the channel buffer
     * @return the message to pass to the next stage
     */
    protected abstract Object decodeProcessed(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception;
}
