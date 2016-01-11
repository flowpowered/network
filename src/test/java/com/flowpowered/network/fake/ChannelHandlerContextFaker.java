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
package com.flowpowered.network.fake;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ChannelHandlerContextFaker {
    private static FakeChannelHandlerContext context = null;
    private static Channel channel = null;
    private static ChannelConfig config = null;
    private static ByteBufAllocator alloc = null;

    public static FakeChannelHandlerContext setup() {
        if (context == null) {
            alloc();
            context = Mockito.mock(FakeChannelHandlerContext.class, Mockito.CALLS_REAL_METHODS);
            channel = Mockito.mock(Channel.class);
            config = Mockito.mock(ChannelConfig.class);
            Mockito.doReturn(channel).when(context).channel();
            Mockito.when(channel.config()).thenReturn(config);
            Mockito.when(config.getAllocator()).thenReturn(alloc);
            Answer<ByteBuf> answer = new Answer<ByteBuf>() {
                             @Override
                             public ByteBuf answer(InvocationOnMock invocation) throws Throwable {
                                 ByteBuf buffer = Unpooled.buffer();
                                 buffer.retain();
                                 return buffer;
                             }
                         };
            Mockito.when(alloc.buffer()).thenAnswer(answer);
            Mockito.when(alloc.buffer(Mockito.anyInt())).thenAnswer(answer);
        }
        return context;
    }

    public static ByteBufAllocator alloc() {
        if (alloc == null) {
            alloc = Mockito.mock(ByteBufAllocator.class);
        }
        return alloc;
    }
}
