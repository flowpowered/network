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

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.channel.Channel;

import com.flowpowered.networking.Message;
import com.flowpowered.networking.protocol.AbstractProtocol;

/**
 * Represents a {@link BasicSession} which has both a {@link State} and {@link SendType}. It can queue messages if needed.
 */
public abstract class PulsingSession extends BasicSession {
    /**
     * A queue of incoming and unprocessed messages
     */
    private final Queue<Message> messageQueue = new ArrayDeque<>();
    /**
     * A queue of outgoing messages that will be sent after the client finishes identification
     */
    private final Queue<Message> sendQueue = new ConcurrentLinkedQueue<>();
    /**
     * The current state.
     */
    private State state = State.EXCHANGE_HANDSHAKE;

    /**
     * Creates a new pulsing session.
     *
     * @param channel The channel associated with this session.
     */
    public PulsingSession(Channel channel, AbstractProtocol bootstrapProtocol) {
        super(channel, bootstrapProtocol);
    }

    /**
     * Gets the state of this session.
     *
     * @return The session's state.
     */
    public State getState() {
        return state;
    }

    /**
     * Sets the state of this session.
     *
     * @param state The new state.
     */
    public void setState(State state) {
        this.state = state;
    }

    public void pulse() {
        Message message;

        if (state == State.OPEN) {
            while ((message = sendQueue.poll()) != null) {
                super.send(message);
            }
        }

        while ((message = messageQueue.poll()) != null) {
            super.messageReceived(message);
        }
    }

    @Override
    public void send(Message message) {
        send(SendType.QUEUE, message);
    }

    public void send(SendType type, Message message) {
        if (message == null) {
            return;
        }
        if (type == SendType.FORCE || this.state == State.OPEN) {
            super.send(message);
        } else if (type == SendType.QUEUE) {
            sendQueue.add(message);
        }
    }

    @Override
    public void sendAll(Message... messages) {
        sendAll(SendType.QUEUE, messages);
    }

    public void sendAll(SendType type, Message... messages) {
        for (Message msg : messages) {
            send(type, msg);
        }
    }

    /**
     * Adds a message to the unprocessed queue.
     *
     * @param message The message.
     */
    @Override
    public void messageReceived(Message message) {
        if (message.isAsync()) {
            super.messageReceived(message);
        } else {
            messageQueue.add(message);
        }
    }

    /**
     * Specifies send behavior
     */
    public static enum SendType {
        /**
         * Messages sent with a SendType of OPEN_ONLY will only send if State is OPEN. Messages will not be
         * queued.
         */
        OPEN_ONLY, /**
         * Messages sent with a SendType of QUEUE will wait until State is OPEN to send. Messages may be queued.
         */
        QUEUE, /**
         * Messages sent with a SendType of FORCE will send as soon as possible regardless of State.
         */
        FORCE
    }

    public static enum State {
        /**
         * In the exchange handshake state, the server is waiting for the client to send its initial handshake
         * packet.
         */
        EXCHANGE_HANDSHAKE, /**
         * In the exchange identification state, the server is waiting for the client to send its identification
         * packet.
         */
        EXCHANGE_IDENTIFICATION, /**
         * In the exchange encryption state, the server is waiting for the client to send its encryption
         * response packet.
         */
        EXCHANGE_ENCRYPTION, /**
         * This state is when a critical message has been sent that must be waited for.
         */
        WAITING, /**
         * Allows messages to be sent.
         */
        OPEN
    }
}
