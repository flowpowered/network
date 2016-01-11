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
package com.flowpowered.network.protocol.simple;

import java.lang.reflect.InvocationTargetException;

import com.flowpowered.network.Codec;
import com.flowpowered.network.Codec.CodecRegistration;
import com.flowpowered.network.Message;
import com.flowpowered.network.MessageHandler;
import com.flowpowered.network.protocol.AbstractProtocol;
import com.flowpowered.network.service.CodecLookupService;
import com.flowpowered.network.service.HandlerLookupService;

import org.slf4j.Logger;

/**
 * A {@code AbstractProtocol} stores {@link Message}s and their respective {@link Codec}s and {@link MessageHandler}s.
 */
public abstract class SimpleProtocol extends AbstractProtocol {
    private final CodecLookupService codecLookup;
    private final HandlerLookupService handlerLookup;

    /**
     * @param name
     * @param maxPackets {@see CodecLookupService}
     */
    public SimpleProtocol(String name, int maxPackets) {
        super(name);
        codecLookup = new CodecLookupService(maxPackets);
        handlerLookup = new HandlerLookupService();
    }

    /**
     * @param name
     * @param maxPackets {@see CodecLookupService}
     * @param logger
     */
    public SimpleProtocol(String name, int maxPackets, Logger logger) {
        super(name, logger);
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
    public <M extends Message> CodecRegistration getCodecRegistration(Class<M> message) {
        return codecLookup.find(message);
    }

    @Override
    public <M extends Message> MessageHandler<?, M> getMessageHandle(Class<M> message) {
        return handlerLookup.find(message);
    }

    public <M extends Message, C extends Codec<? super M>, H extends MessageHandler<?, ? super M>> CodecRegistration registerMessage(Class<M> message, Class<C> codec, Class<H> handler, Integer opcode) {
        try {
            CodecRegistration bind = codecLookup.bind(message, codec, opcode);
            if (bind != null && handler != null) {
                handlerLookup.bind(message, handler);
            }
            return bind;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            getLogger().error("Error registering codec " + codec + ": ", e);  // TODO: Use parametrized message instead of string concatation.
            return null;
        }
    }
}