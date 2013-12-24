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

import io.netty.channel.Channel;

import org.apache.logging.log4j.Level;

import com.flowpowered.networking.Message;
import com.flowpowered.networking.MessageHandler;
import com.flowpowered.networking.protocol.Protocol;

/**
 * Represents a connection to another engine.
 * <br/>
 * Controls the state, protocol and channels of a connection to another engine.
 */
public interface Session {
    /**
     * Passes a message to a session for processing.
     *
     * @param message message to be processed
     */
    public <T extends Message> void messageReceived(T message);

    /**
     * Gets the protocol associated with this session.
     *
     * @return the protocol
     */
    public Protocol getProtocol();

    /**
     * Sends a message across the network.
     *
     * @param message The message.
     */
    public void send(Message message);

    /**
     * Sends any amount of messages to the client.
     *
     * @param messages the messages to send to the client
     */
    public void sendAll(Message... messages);

    /**
     * Closes the session.
     *
     */
    public void disconnect();

    /**
     * Called after the Session has been disconnected, right before the Session is invalidated.
     *
     */
    public void onDisconnect();

    /**
     * Called once the Session is ready for messages.
     *
     */
    public void onReady();

    /**
     * Returns the address of this session.
     *
     * @return The remote address.
     */
    public InetSocketAddress getAddress();

    /**
     * Gets the id for this session
     *
     * @return session id
     */
    public String getSessionId();

    /**
     * Validates that {@code c} is the channel of the session.  The channel of a session never changes.
     *
     * @param c the channel to check
     * @throws IllegalStateException if {@code c} is not the channel of the session
     */
    public void validate(Channel c) throws IllegalStateException;

    /**
     * True if this session is open and connected. If the session is closed, errors will be thrown if messages are attempted to be sent.
     *
     * @return is active
     */
    public boolean isActive();

    public interface UncaughtExceptionHandler {
        /**
         * Called when an exception occurs during session handling
         *
         * @param message the message handler threw an exception on
         * @param handle handler that threw the an exception handling the message
         * @param ex the exception
         */
        public void uncaughtException(Message message, MessageHandler<?> handle, Exception ex);
    }

    /**
     * Gets the uncaught exception handler.
     *
     * <p>Note: the default exception handler is the {@link DefaultUncaughtExceptionHandler}.</p>
     *
     * @return exception handler
     */
    public UncaughtExceptionHandler getUncaughtExceptionHandler();

    /**
     * Sets the uncaught exception handler to be used for this session. Null values are not permitted.
     *
     * <p>Note: to reset the default exception handler, use the{@link DefaultUncaughtExceptionHandler}.</p>
     */
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler);

    public static final class DefaultUncaughtExceptionHandler implements UncaughtExceptionHandler {
        private final Session session;

        public DefaultUncaughtExceptionHandler(Session session) {
            this.session = session;
        }

        @Override
        public void uncaughtException(Message message, MessageHandler<?> handle, Exception ex) {
            session.getProtocol().getLogger().log(Level.ERROR, "Message handler for " + message.getClass().getSimpleName() + " threw exception", ex);
            //session.disconnect("Message handler exception for " + message.getClass().getSimpleName());
            session.disconnect();
        }
    }
}
