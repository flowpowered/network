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
package com.flowpowered.networking.session;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.Channel;

import com.flowpowered.networking.Message;
import com.flowpowered.networking.MessageHandler;
import com.flowpowered.networking.protocol.Protocol;

/**
 * A basic implementation of a {@link Session} which handles and sends messages instantly.
 */
public class BasicSession implements Session {
    /**
     * The Random used for sessionIds.
     */
    private static final Random random = new Random();
    /**
     * The channel associated with this session.
     */
    private final Channel channel;
    /**
     * The random long used for client-server handshake
     */
    private final String sessionId = Long.toString(random.nextLong(), 16).trim();
    /**
     * The protocol for this session
     */
    private Protocol protocol;
    /**
     * Default uncaught exception handler
     */
    private final AtomicReference<UncaughtExceptionHandler> exceptionHandler;

    /**
     * Creates a new session.
     *
     * @param channel The channel associated with this session.
     */
    public BasicSession(Channel channel, Protocol bootstrapProtocol) {
        this.channel = channel;
        this.protocol = bootstrapProtocol;
        this.exceptionHandler = new AtomicReference<UncaughtExceptionHandler>(new DefaultUncaughtExceptionHandler(this));
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(Message message) {
        MessageHandler<Message> handler = (MessageHandler<Message>) protocol.getHandlerLookupService().find(message.getClass());
        if (handler != null) {
            try {
                handler.handle(this, message);
            } catch (Exception e) {
                exceptionHandler.get().uncaughtException(message, handler, e);
            }
        }
    }

    @Override
    public void send(Message message) {
        if (!channel.isActive()) {
            throw new IllegalStateException("Trying to send a message when a session is inactive!");
        }
        try {
            channel.writeAndFlush(message);
        } catch (Exception e) {
            protocol.getLogger().error("Exception when trying to send message, disconnecting.", e);
            disconnect();
        }
    }

    @Override
    public void sendAll(Message... messages) {
        for (Message msg : messages) {
            send(msg);
        }
    }

    /**
     * Returns the address of this session.
     *
     * @return The remote address.
     */
    @Override
    public InetSocketAddress getAddress() {
        SocketAddress addr = channel.remoteAddress();
        if (!(addr instanceof InetSocketAddress)) {
            return null;
        }

        return (InetSocketAddress) addr;
    }

    @Override
    public String toString() {
        return BasicSession.class.getName() + " [address=" + channel.remoteAddress() + "]";
    }

    /**
     * Adds a message to the unprocessed queue.
     *
     * @param message The message.
     */
    @Override
    public void messageReceived(Message message) {
        handleMessage(message);
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public Protocol getProtocol() {
        return this.protocol;
    }

    protected void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public void validate(Channel c) throws IllegalStateException {
        if (c != this.channel) {
            throw new IllegalStateException("Unknown channel for session!");
        }
    }

    @Override
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return exceptionHandler.get();
    }

    @Override
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        if (handler != null) {
            exceptionHandler.set(handler);
        } else {
            throw new IllegalArgumentException("Null uncaught exception handlers are not permitted");
        }
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public void disconnect() {
        channel.close();
    }

    @Override
    public void onDisconnect() {
    }

    @Override
    public void onReady() {
    }
}
