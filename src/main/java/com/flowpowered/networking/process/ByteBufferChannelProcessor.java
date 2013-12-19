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

/**
 * Represents a processor that acts as a pass-through, backed by a byte array
 */
public class ByteBufferChannelProcessor extends CommonChannelProcessor {
    private byte[] internalBuffer;
    private int writePointer;
    private int readPointer;
    private boolean full;

    public ByteBufferChannelProcessor(int capacity) {
        super(capacity);
    }

    @Override
    protected void write(byte[] buf, int length) {
        if (length > buf.length) {
            throw new ArrayIndexOutOfBoundsException(length + " exceeds the size of the byte array " + buf.length);
        }

        int toCopy = length;

        if (internalBuffer == null) {
            internalBuffer = new byte[capacity << 1];
            readPointer = 0;
            writePointer = 0;
            full = false;
        }
        if (freeSpace() < length) {
            throw new IllegalStateException("Internal buffer ran out of memory");
        }
        int toTransfer = Math.min(length, internalBuffer.length - writePointer);
        System.arraycopy(buf, 0, internalBuffer, writePointer, toTransfer);
        writePointer = (writePointer + toTransfer) % internalBuffer.length;

        length -= toTransfer;

        if (length > 0) {
            System.arraycopy(buf, toTransfer, internalBuffer, writePointer, length);
            writePointer = (writePointer + length) % internalBuffer.length;
        }

        if (writePointer == readPointer && toCopy > 0) {
            full = true;
        }
    }

    @Override
    protected int read(byte[] buf) {
        final int toCopy = Math.min(stored(), buf.length);
        final int toTransfer = Math.min(toCopy, internalBuffer.length - readPointer);
        int length = toCopy;

        System.arraycopy(internalBuffer, readPointer, buf, 0, toTransfer);
        readPointer = (readPointer + toTransfer) % internalBuffer.length;

        length -= toTransfer;

        if (length > 0) {
            System.arraycopy(internalBuffer, 0, buf, toTransfer, length);
            readPointer = (readPointer + length) % internalBuffer.length;
        }

        if (toCopy > 0) {
            full = false;
        }
        return toCopy;
    }

    private int stored() {
        if (full) {
            return internalBuffer.length;
        }

        if (writePointer >= readPointer) {
            return writePointer - readPointer;
        }

        return internalBuffer.length - (readPointer - writePointer);
    }

    private int freeSpace() {
        if (full) {
            return 0;
        }

        if (writePointer >= readPointer) {
            return internalBuffer.length - (writePointer - readPointer);
        }

        return readPointer - writePointer;
    }
}
