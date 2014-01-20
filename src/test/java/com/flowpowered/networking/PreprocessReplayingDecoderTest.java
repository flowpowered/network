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

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import com.flowpowered.networking.fake.ChannelHandlerContextFaker;
import com.flowpowered.networking.fake.FakeChannelHandlerContext;
import com.flowpowered.networking.processor.MessageProcessor;
import com.flowpowered.networking.processor.PreprocessReplayingDecoder;
import com.flowpowered.networking.processor.simple.SimpleMessageProcessor;

public class PreprocessReplayingDecoderTest {
    private final int LENGTH = 65536;
    private final int BREAK = 17652;

    @Test
    public void test() throws Exception {
        // Preprocessor basically is split into two parts
        // Part 1 is just a direct copy
        // Part 2 negates all bytes before copying
        Preprocessor p = new Preprocessor(512, BREAK, LENGTH);

        // Set up a fake ChannelHandlerContext
        FakeChannelHandlerContext fake = ChannelHandlerContextFaker.setup();
        fake.setList(new LinkedList<byte[]>());

        Random r = new Random();

        // Get some random bytes for data
        byte[] input = new byte[LENGTH];
        r.nextBytes(input);

        for (int i = 0; i < input.length;) {
            // Simulate real data read
            int burstSize = r.nextInt(512);
            // With a 1/10 chance of having an extra-large burst
            if (r.nextInt(10) == 0) {
                burstSize *= 10;
            }

            // Final burst needs to be clamped
            if (i + burstSize > input.length) {
                burstSize = input.length - i;
            }

            // Write info to a new ByteBuf
            final ByteBuf buf = Unpooled.buffer(burstSize);
            buf.retain();
            buf.writeBytes(input, i, burstSize);
            i += burstSize;

            // Fake a read
            p.channelRead(fake, buf);
        }

        // Get the output data and combine into one array
        List<byte[]> outputList = fake.getList();
        byte[] output = new byte[LENGTH];
        int i = 0;
        for (byte[] array : outputList) {
            for (int j = 0; j < array.length; j++) {
                output[i++] = array[j];
            }
        }

        for (i = 0; i < input.length; i++) {
            byte expected = i < BREAK ? input[i] : (byte) ~input[i];
            if (output[i] != expected) {
                for (int j = i - 10; j <= i + 10; j++) {
                    //System.out.println(j + ") " + Integer.toBinaryString(input[j] & 0xFF) + " " + Integer.toBinaryString(output[j] & 0xFF));
                }
            }

            if (i < BREAK) {
                assertTrue("Input/Output mismatch at position " + i, output[i] == input[i]);
            } else {
                assertTrue("Input/Output mismatch at position " + i + ", after the processor change", output[i] == (byte) ~input[i]);
            }
        }
    }

    private static class Preprocessor extends PreprocessReplayingDecoder {
        private volatile MessageProcessor processor = null;
        private final int breakPoint;
        private final int length;
        private int position = 0;
        private boolean breakOccured;
        private Random r = new Random();

        public Preprocessor(int capacity, int breakPoint, int length) {
            super(capacity);
            this.breakPoint = breakPoint;
            this.length = length;
        }

        @Override
        public Object decodeProcessed(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
            int packetSize = r.nextInt(128) + 1;
            if (r.nextInt(10) == 0) {
                packetSize *= 20;
            }

            if (position + packetSize > breakPoint && !breakOccured) {
                packetSize = breakPoint - position;
            }
            if (position + packetSize > length) {
                packetSize = length - position;
            }

            if (packetSize == 0) {
                return null;
            }

            byte[] buf = new byte[packetSize];

            buffer.readBytes(buf);

            position += packetSize;

            if (position == breakPoint) {
                processor = new NegatingProcessor(512);
                breakOccured = true;
            }

            return buf;
        }

        @Override
        protected MessageProcessor getProcessor() {
            return processor;
        }
    }

    private static class NegatingProcessor extends SimpleMessageProcessor {
        byte[] buffer = new byte[65536];
        int readPointer = 0;
        int writePointer = 0;
        int mask = 0xFFFF;

        public NegatingProcessor(int capacity) {
            super(capacity);
        }

        @Override
        protected void writeEncode(byte[] buf, int length) {
            throw new UnsupportedOperationException("Test not written for encode.");
        }

        @Override
        protected int readEncode(byte[] buf) {
            throw new UnsupportedOperationException("Test not written for encode.");
        }

        @Override
        protected void writeDecode(byte[] buf, int length) {
            for (int i = 0; i < length; i++) {
                buffer[(writePointer++) & mask] = (byte) ~buf[i];
            }
        }

        @Override
        protected int readDecode(byte[] buf) {
            int i;
            for (i = 0; i < buf.length && readPointer < writePointer; i++) {
                buf[i] = buffer[(readPointer++) & mask];
            }
            return i;
        }
    }
}
