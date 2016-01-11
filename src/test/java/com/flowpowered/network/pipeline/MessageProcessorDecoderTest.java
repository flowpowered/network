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

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import com.flowpowered.network.fake.ChannelHandlerContextFaker;
import com.flowpowered.network.fake.FakeChannelHandlerContext;
import com.flowpowered.network.processor.MessageProcessor;
import com.flowpowered.network.processor.simple.SimpleMessageProcessor;

public class MessageProcessorDecoderTest {
    private final int LENGTH = 65536;
    private final int BREAK = 17652;

    @Test
    public void test() throws Exception {
        // Preprocessor basically is split into two parts
        // Part 1 is just a direct copy
        // Part 2 negates all bytes before copying
        final AtomicReference<MessageProcessor> processor = new AtomicReference<>();
        MessageProcessorDecoder processorDecoder = new MessageProcessorDecoder(null) {
            @Override
            protected MessageProcessor getProcessor() {
                return processor.get();
            }
        };

        // Set up a fake ChannelHandlerContext
        FakeChannelHandlerContext fake = ChannelHandlerContextFaker.setup();
        AtomicReference<ByteBuf> ref = new AtomicReference<>();
        fake.setReference(ref);
        LinkedList<byte[]> outputList = new LinkedList<byte[]>();

        Random r = new Random();

        // Get some random bytes for data
        byte[] input = new byte[LENGTH];
        r.nextBytes(input);

        boolean breakOccured = false;
        int position = 0;

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

            // And we can't negate in the middle of a burst
            if (i + burstSize > BREAK && !breakOccured) {
                burstSize = BREAK - i;
            }

            // Write info to a new ByteBuf
            final ByteBuf buf = Unpooled.buffer(burstSize);
            buf.retain();
            buf.writeBytes(input, i, burstSize);
            i += burstSize;

            // Fake a read
            processorDecoder.channelRead(fake, buf);

            final ByteBuf returned = ref.get();

            while (returned != null && true) {
                int packetSize = r.nextInt(128) + 1;
                if (r.nextInt(10) == 0) {
                    packetSize *= 20;
                }

                if (packetSize > returned.readableBytes()) {
                    packetSize = returned.readableBytes();
                }
                if (position + packetSize > BREAK && !breakOccured) {
                    packetSize = BREAK - position;
                }
                if (position + packetSize > LENGTH) {
                    packetSize = LENGTH - position;
                }

                if (packetSize == 0) {
                    break;
                }

                byte[] array = new byte[packetSize];

                returned.readBytes(array);
                position += packetSize;

                if (position == BREAK) {
                    processor.set(new NegatingProcessor(512));
                    breakOccured = true;
                }
                outputList.add(array);
            }
        }


        // Get the output data and combine into one array
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
                for (int j = Math.max(0, i - 10); j <= i + 10; j++) {
                    System.out.println(j + ") " + Integer.toBinaryString(j < BREAK ? input[j] : (byte) ~input[j]) + " " + Integer.toBinaryString(output[j]));
                }
            }

            if (i < BREAK) {
                assertTrue("Input/Output mismatch at position " + i + ". Expected " + input[i] + " but got " + output[i] + ". Break is: " + BREAK, output[i] == input[i]);
            } else {
                assertTrue("Input/Output mismatch at position " + i + ", after the processor change. Expected " + (byte) ~input[i] + " but got " + output[i] + ". Break is: " + BREAK, output[i] == (byte) ~input[i]);
            }
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
