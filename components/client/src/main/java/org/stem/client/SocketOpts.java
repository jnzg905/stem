/*
 * Copyright 2014 Alexey Plotnik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stem.client;

public class SocketOpts {

    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 3000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 8000;

    private volatile int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MS;
    private volatile int readTimeoutMs = DEFAULT_READ_TIMEOUT_MS;
    private volatile Boolean keepAlive;
    private volatile Boolean reuseAddress;
    private volatile Integer soLinger;
    private volatile Boolean tcpNoDelay;
    private volatile Integer receiveBufferSize;
    private volatile Integer sendBufferSize;

    public SocketOpts() {
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public SocketOpts setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        return this;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public SocketOpts setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
        return this;
    }

    public Boolean getKeepAlive() {
        return keepAlive;
    }

    public SocketOpts setKeepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public Boolean getReuseAddress() {
        return reuseAddress;
    }

    public SocketOpts setReuseAddress(Boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
        return this;
    }

    public Integer getSoLinger() {
        return soLinger;
    }

    public SocketOpts setSoLinger(Integer soLinger) {
        this.soLinger = soLinger;
        return this;
    }

    public Boolean getTcpNoDelay() {
        return tcpNoDelay;
    }

    public SocketOpts setTcpNoDelay(Boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
        return this;
    }

    public Integer getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public SocketOpts setReceiveBufferSize(Integer receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
        return this;
    }

    public Integer getSendBufferSize() {
        return sendBufferSize;
    }

    public SocketOpts setSendBufferSize(Integer sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }
}
