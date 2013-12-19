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

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import com.flowpowered.networking.protocol.Protocol;

/**
 * This class provides a way to store Protocols by name and {@link SocketAddress}.
 *
 */
public class ProtocolRegistry {
    private final ConcurrentHashMap<String, Protocol> names = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SocketAddress, Protocol> sockets = new ConcurrentHashMap<>();

    /**
     * Registers a Protocol under its name
     *
     * @param protocol the Protocol
     */
    public void registerProtocol(SocketAddress adress, Protocol protocol) {
        this.names.put(protocol.getName(), protocol);
        this.sockets.put(adress, protocol);
    }

    /**
     * Gets the Protocol associated with a particular id
     *
     * @param name the id
     * @return the Protocol
     */
    public Protocol getProtocol(String name) {
        return this.names.get(name);
    }

    /**
     * Gets the Protocol associated with a particular address
     *
     * @param address the address
     * @return the Protocol
     */
    public Protocol getProtocol(SocketAddress address) {
        return this.sockets.get(address);
    }

    /**
     * Returns all protocols currently registered. The returned collection is unmodifiable.
     *
     * @return All registered protocols
     */
    public Collection<Protocol> getProtocols() {
        return Collections.unmodifiableCollection(this.names.values());
    }

}
