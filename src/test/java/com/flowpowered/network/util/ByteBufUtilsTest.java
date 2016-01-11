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
package com.flowpowered.network.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ByteBufUtilsTest {
    @Test
    public void testVarInt() throws Exception {
        final ByteBuf test = Unpooled.buffer();
        ByteBufUtils.writeVarInt(test, 1);
        final int varInt = ByteBufUtils.readVarInt(test);
        if (varInt != 1) {
            fail("The buffer had 1 wrote to it but received " + varInt + " instead!");
        }
    }

    @Test
    public void testUtf8() throws Exception {
        final ByteBuf test = Unpooled.buffer();
        ByteBufUtils.writeUTF8(test, "Hello");
        final String utf8String = ByteBufUtils.readUTF8(test);
        if (!"Hello".equals(utf8String)) {
            fail("The buffer had hello wrote to it but received " + utf8String + " instead!");
        }
        boolean exceptionThrown = false;
        try {
            ByteBufUtils.writeUTF8(test, new String(new byte[Short.MAX_VALUE + 1]));
        } catch (Exception ignore) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            fail("Writing more than Short.MAX_VALUE as a UTF8 String to the ByteBuf should have thrown an exception but it did not!");
        }
    }
}
