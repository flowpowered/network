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

import com.flowpowered.networking.Codec;
import com.flowpowered.networking.Codec.CodecRegistration;
import com.flowpowered.networking.Message;
import com.flowpowered.networking.exception.IllegalOpcodeException;

/**
 * A class used to lookup message codecs.
 */
public class CodecLookupService {
    /**
     * A lookup table for the Message classes mapped to their Codec.
     */
    private final ConcurrentMap<Class<? extends Message>, CodecRegistration> messages;
    /**
     * Lookup table for opcodes mapped to their codecs.
     */
    private final ConcurrentMap<Integer, Codec> opcodes;
    /**
     * Stores the next opcode available.
     */
    private final AtomicInteger nextId;
     
    /**
     * The {@link CodecLookupService} stores the codecs available in the protocol. Codecs can be found using either the class of the message they represent or their message's opcode.
     *
     * @param size The maximum number of message types
     */
    public CodecLookupService() {
        messages = new ConcurrentHashMap<>();
        opcodes = new ConcurrentHashMap<>();
        nextId = new AtomicInteger(0);
    }

    /**
     * Binds a codec by adding entries for it to the tables. TODO: if a dynamic opcode is registered then a static opcode tries to register, reassign dynamic. TODO: if a static opcode is registered then
     * a static opcode tries to register, throw exception
     *
     * @param messageClazz The message's class
     * @param codecClazz The codec's class.
     * @param opcode the opcode to register with, or null if the codec should be dynamic
     * @param <M> The type of message
     * @param <C> The type of codec.
     * @return the registration object of the codec
     * @throws InstantiationException if the codec could not be instantiated.
     * @throws IllegalAccessException if the codec could not be instantiated due to an access violation.
     */
    @SuppressWarnings("unchecked")
    public <M extends Message, C extends Codec<? super M>> CodecRegistration bind(Class<M> messageClazz, Class<C> codecClazz, Integer opcode) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        CodecRegistration reg = messages.get(messageClazz);
        if (reg != null) {
            return reg;
        }
        C codec;
        try {
            Constructor<C> con = codecClazz.getConstructor();
            con.setAccessible(true);
            codec = con.newInstance();
        } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Codec could not be created!", e);
        }
        if (opcode != null) {
            if (opcode < 0) {
                throw new IllegalArgumentException("Opcode must either be null or greater than or equal to 0!");
            }
        } else {
            int id;
            try {
                do {
                    id = nextId.getAndIncrement();
                } while (opcodes.get(id) != null);
            } catch (IndexOutOfBoundsException ioobe) {
                throw new IllegalStateException("Ran out of Ids!", ioobe);
            }
            opcode = id;
        }
        if (opcodes.get(opcode) != null && opcodes.get(opcode).getClass() != codecClazz) {
            throw new IllegalStateException("Trying to bind an opcode where one already exists. New: " + codecClazz.getSimpleName() + " Old: " + reg.getCodec().getClass().getSimpleName());
        }
        reg = new CodecRegistration(opcode, codec);
        opcodes.put(opcode, codec);
        messages.put(messageClazz, reg);
        return reg;
    }

    /**
     * Retrieves the {@link Codec} from the lookup table
     *
     * @param opcode The opcode which the codec uses
     * @return The codec, null if not found.
     */
    public Codec<?> find(int opcode) throws IllegalOpcodeException {
        Codec<?> c = opcodes.get(opcode);
        if (c == null) {
            throw new IllegalOpcodeException("Opcode " + opcode + " is not bound!");
        }
        return c;
    }

    /**
     * Finds a codec by message class.
     *
     * @param clazz The message class.
     * @param <M> The type of message.
     * @return The codec, or {@code null} if it could not be found.
     */
    @SuppressWarnings("unchecked")
    public <M extends Message> CodecRegistration find(Class<M> clazz) {
        return messages.get(clazz);
    }
}
