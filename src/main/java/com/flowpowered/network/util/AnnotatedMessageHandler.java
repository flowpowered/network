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
package com.flowpowered.network.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.flowpowered.network.Message;

/**
 * A class for handling messages with annotated methods referred to as handles. The methods need to be annotated with {@link com.flowpowered.network.util.AnnotatedMessageHandler.Handle} and should only
 * have one parameter, the message, which needs to be a subclass of {@link com.flowpowered.network.Message}. These should be instance methods inside any class. When creating the annotated message
 * handler, an instance of the class containing the method is passed to the constructor. Handle methods will be called from this instance when handling messages. To handle a message, simply call
 * {@link #handle(com.flowpowered.network.Message)} with the said message as a parameter. The thread calling {@link #handle(com.flowpowered.network.Message)} is the same that calls the handle
 * method.
 */
public class AnnotatedMessageHandler {
    private final Map<Class<?>, Method> handles = Collections.synchronizedMap(new HashMap<Class<?>, Method>());
    private final Object handler;

    /**
     * Constructs a new annotated message handler using the passed object as the source of handle methods.
     *
     * @param handler The message handler
     */
    public AnnotatedMessageHandler(Object handler) {
        this.handler = handler;
        registerHandlers();
    }

    /**
     * Handles the given message.
     *
     * @param message The message to handle
     */
    public void handle(Message message) {
        final Method handle = handles.get(message.getClass());
        if (handle == null) {
            throw new IllegalArgumentException("No handle for message type [" + message.getClass().getName() + "]");
        }
        try {
            handle.invoke(handler, message);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to handle message [" + message + "]", ex);
        }
    }

    private void registerHandlers() {
        for (Method method : handler.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Handle.class)) {
                final Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new IllegalStateException("Expected only 1 parameter for message handler method [" + method.getName() + "]");
                }
                final Class<?> messageType = parameterTypes[0];
                if (!Message.class.isAssignableFrom(messageType)) {
                    throw new IllegalStateException("Expected parameter to be a subclass of Message for handler method [" + method.getName() + "]");
                }
                method.setAccessible(true);
                handles.put(messageType, method);
            }
        }
    }

    /**
     * The annotation to mark message handle methods.
     */
    @Target (ElementType.METHOD)
    @Retention (RetentionPolicy.RUNTIME)
    public static @interface Handle {
    }
}
