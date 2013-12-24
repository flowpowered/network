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
package com.flowpowered.networking.protocol;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import io.netty.buffer.ByteBuf;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.flowpowered.commons.Named;
import com.flowpowered.commons.StringToUniqueIntegerMap;
import com.flowpowered.commons.store.MemoryStore;
import com.flowpowered.networking.Codec;
import com.flowpowered.networking.Message;
import com.flowpowered.networking.MessageHandler;
import com.flowpowered.networking.exception.UnknownPacketException;

/**
 * A {@code Protocol} stores {@link Message}s and their respective {@link Codec}s and {@link MessageHandler}s.
 * It also stores to what port the protocol should be bound to.
 */
public abstract class Protocol implements Named {
    private final CodecLookupService codecLookup;
    private final HandlerLookupService handlerLookup;
    private final String name;
    private final int defaultPort;
    private final Logger logger;

    public Protocol(String name, int defaultPort, int maxPackets) {
        this(name, defaultPort, maxPackets, LogManager.getLogger("Protocol." + name));
    }

    public Protocol(String name, int defaultPort, int maxPackets, Logger logger) {
        this.name = name;
        StringToUniqueIntegerMap dynamicPacketLookup = new StringToUniqueIntegerMap(null, new MemoryStore<Integer>(), maxPackets, maxPackets, this.name + "ProtocolDynamicPackets");
        codecLookup = new CodecLookupService(dynamicPacketLookup, maxPackets);
        handlerLookup = new HandlerLookupService();
        this.defaultPort = defaultPort;
        this.logger = logger;
    }

    /**
     * Gets the handler lookup service associated with this Protocol
     *
     * @return the handler lookup service
     */
    public HandlerLookupService getHandlerLookupService() {
        return handlerLookup;
    }

    /**
     * Gets the codec lookup service associated with this Protocol
     *
     * @return the codec lookup service
     */
    public CodecLookupService getCodecLookupService() {
        return codecLookup;
    }

    public <M extends Message, C extends Codec<M>, H extends MessageHandler<M>> C registerMessage(Class<C> codec, Class<H> handler) {
        try {
            C bind = codecLookup.bind(codec);
            if (bind != null && handler != null) {
                handlerLookup.bind(bind.getMessage(), handler);
            }
            return bind;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            logger.log(Level.ERROR, "Error registering codec " + codec + ": ", e);
            return null;
        }
    }

    /**
     * Gets the name of the Protocol
     *
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * The default port is the port used when autogenerating default bindings for this protocol and in the client when no port is given.
     *
     * @return The default port
     */
    public int getDefaultPort() {
        return defaultPort;
    }

    /**
     * Returns the logger for this protocol.
     * 
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Allows applying a wrapper to messages with dynamically allocated id's, in case this protocol needs to provide special treatment for them.
     *
     * @param dynamicMessage The message with a dynamically-allocated codec
     * @return The new message
     */
    public <T extends Message> Message getWrappedMessage(T dynamicMessage) throws IOException {
        return dynamicMessage;
    }

    /**
     * Read a packet header from the buffer. If a codec is not known, throw a {@link UnknownPacketException}
     *
     * @param buf The buffer to read from
     * @return The correct codec
     * @throws UnknownPacketException when the opcode does not have an associated codec
     */
    public abstract Codec<?> readHeader(ByteBuf buf) throws UnknownPacketException;

    /**
     * Writes a packet header to a new buffer.
     *
     * @param codec The codec the message was written with
     * @param data The data from the encoded message
     * @param header the buffer which to write the header to
     * @return The buffer with the packet header
     */
    public abstract ByteBuf writeHeader(Codec<?> codec, ByteBuf data, ByteBuf header);
}
