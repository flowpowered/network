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
package com.flowpowered.networking.pipeline;

import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.flowpowered.networking.ConnectionManager;
import com.flowpowered.networking.Message;
import com.flowpowered.networking.session.Session;

/**
 * A {@link SimpleChannelUpstreamHandler} which processes incoming network events.
 */
public class MessageHandler extends SimpleChannelInboundHandler<Message> {
    /**
     * The associated session
     */
    private final AtomicReference<Session> session = new AtomicReference<>(null);
    private final ConnectionManager connectionManager;

    /**
     * Creates a new network event handler.
     */
    public MessageHandler(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel c = ctx.channel();
        setSession(connectionManager.newSession(c));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel c = ctx.channel();
        Session session = this.session.get();
        // TODO needed?
        session.validate(c);
        session.onDisconnect();
        connectionManager.sessionInactivated(session);
        getLogger().info("Channel disconnected: " + c + ".");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message i) {
        Session session = this.session.get();
        session.validate(ctx.channel());
        session.messageReceived(i);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel c = ctx.channel();
        getLogger().log(Level.WARN, "Exception caught, closing channel: " + c + "...", cause);
        c.close();
    }

    public Session getSession() {
        return session.get();
    }

    public void setSession(Session session) {
        if (!this.session.compareAndSet(null, session)) {
            throw new IllegalStateException("Session may not be set more than once");
        }
    }

    protected Logger getLogger() {
        String loggerName = "";
        if (session.get() != null) {
            Logger protocolLogger = session.get().getProtocol().getLogger();
            // TODO: Maybe we should just use the protocolLogger if present?
            loggerName = protocolLogger != null ? protocolLogger.getName() + "." : "";
        }
        return LogManager.getLogger(loggerName + getClass().getSimpleName());
    }
}
