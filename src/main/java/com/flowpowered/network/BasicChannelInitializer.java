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
package com.flowpowered.network;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import com.flowpowered.network.pipeline.MessageDecoder;
import com.flowpowered.network.pipeline.MessageEncoder;
import com.flowpowered.network.pipeline.MessageHandler;
import com.flowpowered.network.pipeline.MessageProcessorDecoder;
import com.flowpowered.network.pipeline.MessageProcessorEncoder;

/**
 * Used to initialize the channels.
 */
public class BasicChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final ConnectionManager connectionManager;

    public BasicChannelInitializer(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    protected final void initChannel(SocketChannel c) {
        MessageHandler handler = new MessageHandler(connectionManager);
        MessageProcessorDecoder processorDecoder = new MessageProcessorDecoder(handler);
        MessageProcessorEncoder processorEncoder = new MessageProcessorEncoder(handler);
        MessageDecoder decoder = new MessageDecoder(handler);
        MessageEncoder encoder = new MessageEncoder(handler);

        c.pipeline()
                .addLast("processorDecoder", processorDecoder)
                .addLast("decoder", decoder)
                .addLast("processorEncoder", processorEncoder)
                .addLast("encoder", encoder)
                .addLast("handler", handler);
    }
}
