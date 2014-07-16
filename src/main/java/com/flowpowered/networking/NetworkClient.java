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

import java.net.SocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * This class defines an easy, general way to start a client. It is recommended that any clients use or extend this class.
 */
public abstract class NetworkClient implements ConnectionManager {
    private final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NetworkClient() {
        bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel.class)
            .handler(new BasicChannelInitializer(this));
    }

    public ChannelFuture connect(final SocketAddress address) {
        return bootstrap.connect(address).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> f) throws Exception {
                if (f.isSuccess()) {
                    onConnectSuccess(address);
                } else {
                    onConnectFailure(address, f.cause());
                }
            }
        });
    }

    /**
     * Sets an {@link ChannelOption} to apply prior to connecting. After a connection has been established, this method is useless; an Exception *may* be thrown on it's use.
     * @param <T>
     * @param option
     * @param value
     */
    public <T> void preConnectOption(ChannelOption<T> option, T value) {
        bootstrap.option(option, value);
    }

    /**
     * Called when a connection has successfully been made.
     * @param address The address we succesfully connected to.
     */
    public void onConnectSuccess(SocketAddress address) {
    }

    /**
     * Called when a connection cannot be made.
     * @param address The address that we attempted to connect to.
     * @param t The cause of why the connection couldn't be made. This can be null.
     */
    public void onConnectFailure(SocketAddress address, Throwable t) {
    }

    @Override
    public void shutdown() {
        workerGroup.shutdownGracefully();
    }
}
