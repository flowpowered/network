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

import com.flowpowered.networking.Message;
import com.flowpowered.networking.protocol.Protocol;
import com.flowpowered.networking.protocol.AbstractProtocol;
import org.slf4j.Logger;

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
    <T extends Message> void messageReceived(T message);

    /**
     * Gets the protocol associated with this session.
     *
     * @return the protocol
     */
    Protocol getProtocol();

    /**
     * Sends a message across the network.
     *
     * @param message The message.
     */
    void send(Message message);

    /**
     * Sends any amount of messages to the client.
     *
     * @param messages the messages to send to the client
     */
    void sendAll(Message... messages);

    /**
     * Closes the session.
     *
     */
    void disconnect();

    /**
     * Called after the Session has been disconnected, right before the Session is invalidated.
     *
     */
    void onDisconnect();

    /**
     * Called once the Session is ready for messages.
     *
     */
    void onReady();

    /**
     * Called when a throwable is thrown in the pipeline.
     *
     * @param throwable 
     */
    void onThrowable(Throwable throwable);

    /**
     * Returns the address of this session.
     *
     * @return The remote address.
     */
    InetSocketAddress getAddress();

    /**
     * Gets the id for this session
     *
     * @return session id
     */
    String getSessionId();

    /**
     * Validates that {@code c} is the channel of the session.  The channel of a session never changes.
     *
     * @param c the channel to check
     * @throws IllegalStateException if {@code c} is not the channel of the session
     */
    void validate(Channel c) throws IllegalStateException;

    /**
     * True if this session is open and connected. If the session is closed, errors will be thrown if messages are attempted to be sent.
     *
     * @return is active
     */
    boolean isActive();

    Logger getLogger();
}
