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
package com.flowpowered.networking;

import java.util.Random;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import com.flowpowered.networking.fake.ChannelHandlerContextFaker;
import com.flowpowered.networking.process.ByteBufferChannelProcessor;

public class ByteBufferChannelProcessorTest {
    private final int LENGTH = 65536;
    Thread mainThread;

    @Test
    public void randomPassthrough() {

        mainThread = Thread.currentThread();

        ByteBuf buffer = Unpooled.buffer(2048);

        ChannelHandlerContext ctx = ChannelHandlerContextFaker.setup();

        ByteBufferChannelProcessor processor = new ByteBufferChannelProcessor(256);

        byte[] input = new byte[LENGTH];
        byte[] output = new byte[LENGTH];

        Random r = new Random();

        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) (r.nextInt());
        }

        int writePointer = 0;
        int readPointer = 0;

        int pass = 0;

        while (writePointer < LENGTH && (pass++) < 512) {

            int toWrite = r.nextInt(512);

            if (r.nextInt(10) == 0) {
                // simulate "large" packets
                toWrite *= 10;
            }

            if (toWrite > buffer.writableBytes()) {
                toWrite = buffer.writableBytes();
            }
            if (toWrite > LENGTH - writePointer) {
                toWrite = LENGTH - writePointer;
            }

            //System.out.println("Writing block of size " + toWrite);

            buffer.writeBytes(input, writePointer, toWrite);
            writePointer += toWrite;

            ByteBuf buf = Unpooled.buffer();
            ByteBuf outputBuffer = processor.process(ctx, buffer, buf);

            buffer.discardReadBytes();

            while (outputBuffer.isReadable()) {
                int toRead = r.nextInt(768);
                if (toRead > outputBuffer.readableBytes()) {
                    toRead = outputBuffer.readableBytes();
                }
                //System.out.println("ToRead: " + toRead + " of " + outputBuffer.readableBytes());
                outputBuffer.readBytes(output, readPointer, toRead);
                readPointer += toRead;
                outputBuffer.discardReadBytes();
            }
        }

        for (int i = 0; i < input.length; i++) {
            assertTrue("Mismatch at position " + i, input[i] == output[i]);
        }
    }
}
