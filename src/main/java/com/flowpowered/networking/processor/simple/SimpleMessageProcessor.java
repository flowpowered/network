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
package com.flowpowered.networking.processor.simple;

import com.flowpowered.networking.processor.MessageProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Bridge class for passing ByteBufs through byte array read/write processing. This is only only one {@link DividedMessageProcessorPart} and must use a {@link DividedMessageProcessor}
 */
public abstract class SimpleMessageProcessor implements MessageProcessor {
    protected final int capacity;
    private final byte[] decodingByteBuffer;
    private final byte[] encodingByteBuffer;

    public SimpleMessageProcessor(int capacity) {
        this.capacity = capacity;
        this.decodingByteBuffer = new byte[capacity];
        this.encodingByteBuffer = new byte[capacity];
    }

    @Override
    public final synchronized void processEncode(ChannelHandlerContext ctx, final ByteBuf input, ByteBuf buffer) {
        int remaining;
        while ((remaining = input.readableBytes()) > 0) {
            int clamped = Math.min(remaining, capacity);
            input.readBytes(encodingByteBuffer, 0, clamped);
            writeEncode(encodingByteBuffer, clamped);
            int read;
            while ((read = readDecode(encodingByteBuffer)) > 0) {
                buffer.writeBytes(encodingByteBuffer, 0, read);
            }
        }
    }

    /**
     * Writes data to the processor<br> <br> This method does not need to be thread safe
     *
     * @param buf a buffer containing the data
     * @param length the length of the data to process
     */
    protected abstract void writeEncode(byte[] buf, int length);

    /**
     * Reads the data from the processor into the given array<br> <br> This method does not need to be thread safe
     *
     * @param buf the byte array to process the data to
     * @return the number of bytes written
     */
    protected abstract int readEncode(byte[] buf);

    @Override
    public final synchronized void processDecode(ChannelHandlerContext ctx, final ByteBuf input, ByteBuf buffer) {
        int remaining;
        while ((remaining = input.readableBytes()) > 0) {
            int clamped = Math.min(remaining, capacity);
            input.readBytes(decodingByteBuffer, 0, clamped);
            writeDecode(decodingByteBuffer, clamped);
            int read;
            while ((read = readDecode(decodingByteBuffer)) > 0) {
                buffer.writeBytes(decodingByteBuffer, 0, read);
            }
        }
    }

    /**
     * Writes data to the processor<br> <br> This method does not need to be thread safe
     *
     * @param buf a buffer containing the data
     * @param length the length of the data to process
     */
    protected abstract void writeDecode(byte[] buf, int length);

    /**
     * Reads the data from the processor into the given array<br> <br> This method does not need to be thread safe
     *
     * @param buf the byte array to process the data to
     * @return the number of bytes written
     */
    protected abstract int readDecode(byte[] buf);
}
