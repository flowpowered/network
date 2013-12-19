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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Bridge class for passing ByteBufs through byte array read/write processing
 */
public abstract class CommonChannelProcessor implements ChannelProcessor {
    protected final int capacity;
    private final byte[] byteBuffer;

    public CommonChannelProcessor(int capacity) {
        this.capacity = capacity;
        this.byteBuffer = new byte[capacity];
    }

    @Override
    public final synchronized ByteBuf process(ChannelHandlerContext ctx, final ByteBuf input, ByteBuf buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer cannot be null!");
        }
        int remaining;
        while ((remaining = input.readableBytes()) > 0) {
            int clamped = Math.min(remaining, capacity);
            input.readBytes(byteBuffer, 0, clamped);
            write(byteBuffer, clamped);
            int read;
            while ((read = read(byteBuffer)) > 0) {
                buffer.writeBytes(byteBuffer, 0, read);
            }
        }

        return buffer;
    }

    /**
     * Writes data to the processor<br> <br> This method does not need to be thread safe
     *
     * @param buf a buffer containing the data
     * @param length the length of the data to process
     */
    protected abstract void write(byte[] buf, int length);

    /**
     * Reads the data from the processor into the given array<br> <br> This method does not need to be thread safe
     *
     * @param buf the byte array to process the data to
     * @return the number of bytes written
     */
    protected abstract int read(byte[] buf);
}
