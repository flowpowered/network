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
package com.flowpowered.networking.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.flowpowered.commons.StringToUniqueIntegerMap;
import com.flowpowered.networking.Codec;
import com.flowpowered.networking.Message;
import com.flowpowered.networking.exception.IllegalOpcodeException;

/**
 * A class used to lookup message codecs.
 */
public class CodecLookupService {
    /**
     * A lookup table for the Message classes mapped to their Codec.
     */
    private final ConcurrentMap<Class<? extends Message>, Codec<?>> messages;
    /**
     * A lookup table for the Message classes mapped to their Codec.
     */
    private final ConcurrentMap<Class<? extends Codec<?>>, Codec<?>> codecs;
    /**
     * Lookup table for opcodes mapped to their codecs.
     */
    private final Codec<?>[] opcodeTable;
    /**
     * Stores the next opcode available.
     */
    private final AtomicInteger nextId;
     
    /**
     * The {@link CodecLookupService} stores the codecs available in the protocol. Codecs can be found using either the class of the message they represent or their message's opcode.
     *
     * @param dynamicPacketMap - The dynamic opcode map
     * @param size The maximum number of message types
     */
    public CodecLookupService(int size) {
        messages = new ConcurrentHashMap<>(size, 1.0f);
        codecs = new ConcurrentHashMap<>(size, 1.0f);
        opcodeTable = new Codec<?>[size];
        nextId = new AtomicInteger(0);
    }

    /**
     * Binds a codec by adding entries for it to the tables. TODO: if a dynamic opcode is registered then a static opcode tries to register, reassign dynamic. TODO: if a static opcode is registered then
     * a static opcode tries to register, throw exception
     *
     * @param clazz The codec's class.
     * @param <T> The type of message
     * @param <C> The type of codec.
     * @throws InstantiationException if the codec could not be instantiated.
     * @throws IllegalAccessException if the codec could not be instantiated due to an access violation.
     */
    @SuppressWarnings("unchecked")
    public <T extends Message, C extends Codec<T>> C bind(Class<C> clazz) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        C codec = (C) codecs.get(clazz);
        if (codec != null) {
            return codec;
        }
        try {
            codec = clazz.getConstructor().newInstance();
            final Codec<?> prevCodec = opcodeTable[codec.getOpcode()];
            if (prevCodec != null) {
                throw new IllegalStateException("Trying to bind a static opcode where one already exists. Static: " + clazz.getSimpleName() + " Other: " + prevCodec.getClass().getSimpleName());
            }
        } catch (NoSuchMethodException e) {
            try {
                Constructor<C> constructor = clazz.getConstructor(int.class);
                int id;
                try {
                    do {
                        id = nextId.getAndIncrement();
                    } while (opcodeTable[id] != null);
                } catch (IndexOutOfBoundsException ioobe) {
                    throw new IllegalStateException("Ran out of Ids!", ioobe);
                }
                codec = constructor.newInstance(id);
            } catch (NoSuchMethodException e1) {
                IllegalArgumentException iae = new IllegalArgumentException("Codec must either have a zero arg or single int arg constructor!", e1);
                iae.addSuppressed(e);
                throw iae;
            }
        }
        opcodeTable[codec.getOpcode()] = codec;
        messages.put(codec.getMessage(), codec);
        codecs.put(clazz, codec);
        return codec;
    }

    /**
     * Retrieves the {@link Codec} from the lookup table
     *
     * @param opcode The opcode which the codec uses
     * @return The codec, null if not found.
     */
    public Codec<? extends Message> find(int opcode) throws IllegalOpcodeException {
        if (opcode < 0 || opcode >= opcodeTable.length) {
            throw new IllegalOpcodeException("Opcode " + opcode + " is out of bounds");
        }
        Codec<? extends Message> c = opcodeTable[opcode];
        if (c == null) {
            throw new IllegalOpcodeException("Opcode " + opcode + " is not bound!");
        }
        return c;
    }

    /**
     * Finds a codec by message class.
     *
     * @param clazz The message class.
     * @param <T> The type of message.
     * @return The codec, or {@code null} if it could not be found.
     */
    @SuppressWarnings("unchecked")
    public <T extends Message> Codec<T> find(Class<T> clazz) {
        return (Codec<T>) codecs.get(clazz);
    }
}
