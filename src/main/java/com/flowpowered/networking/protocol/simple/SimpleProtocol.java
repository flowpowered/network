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
package com.flowpowered.networking.protocol.simple;

import java.lang.reflect.InvocationTargetException;

import com.flowpowered.networking.Codec;
import com.flowpowered.networking.Message;
import com.flowpowered.networking.MessageHandler;
import com.flowpowered.networking.protocol.AbstractProtocol;
import com.flowpowered.networking.service.CodecLookupService;
import com.flowpowered.networking.service.HandlerLookupService;

import org.slf4j.Logger;

/**
 * A {@code AbstractProtocol} stores {@link Message}s and their respective {@link Codec}s and {@link MessageHandler}s.
 */
public abstract class SimpleProtocol extends AbstractProtocol {
    private final CodecLookupService codecLookup;
    private final HandlerLookupService handlerLookup;

    public SimpleProtocol(String name, int defaultPort, int maxPackets) {
        super(name, defaultPort);
        codecLookup = new CodecLookupService(maxPackets);
        handlerLookup = new HandlerLookupService();
    }

    /**
     * @param name
     * @param defaultPort
     * @param maxPackets this is one more than the maximum packet id
     * @param logger
     */
    public SimpleProtocol(String name, int defaultPort, int maxPackets, Logger logger) {
        super(name, defaultPort, logger);
        codecLookup = new CodecLookupService(maxPackets);
        handlerLookup = new HandlerLookupService();
    }

    /**
     * Gets the handler lookup service associated with this AbstractProtocol
     *
     * @return the handler lookup service
     */
    protected HandlerLookupService getHandlerLookupService() {
        return handlerLookup;
    }

    /**
     * Gets the codec lookup service associated with this AbstractProtocol
     *
     * @return the codec lookup service
     */
    protected CodecLookupService getCodecLookupService() {
        return codecLookup;
    }

    @Override
    public <M extends Message> Codec<M> getCodec(Class<M> message) {
        return codecLookup.find(message);
    }

    @Override
    public <T extends Message> MessageHandler<T> getMessageHandle(Class<T> message) {
        return handlerLookup.find(message);
    }

    public <M extends Message, C extends Codec<M>, H extends MessageHandler<M>> C registerMessage(Class<C> codec, Class<H> handler) {
        try {
            C bind = codecLookup.bind(codec);
            if (bind != null && handler != null) {
                handlerLookup.bind(bind.getMessage(), handler);
            }
            return bind;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            getLogger().error("Error registering codec " + codec + ": ", e);  // TODO: Use parametrized message instead of string concatation.
            return null;
        }
    }
}