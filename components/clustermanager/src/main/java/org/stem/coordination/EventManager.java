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

package org.stem.coordination;

import org.stem.domain.Cluster;
import org.stem.exceptions.StemException;

import java.util.UUID;

public class EventManager {

    public static final EventManager instance;

    static {
        instance = new EventManager();
    }

    private final ZookeeperClient client;

    public EventManager() {
        try {
            this.client = ZookeeperFactoryCached.newClient(Cluster.instance.descriptor().getZookeeperEndpoint());
        } catch (ZooException e) {
            throw new StemException("Can not create EventManager instance", e);
        }
    }

    public EventFuture createSubscription(Event.Type type) throws Exception {
        return createSubscription(type, UUID.randomUUID());
    }

    public EventFuture createSubscription(Event.Type type, UUID id) throws Exception {
        Event event = Event.create(type, id);
        client.createNode(ZookeeperPaths.ASYNC_REQUESTS, event);
        return new EventFuture(event, this.client);
    }

    public UUID createSubscription(Event request) throws Exception {
        client.createNode(ZookeeperPaths.ASYNC_REQUESTS, request);
        return request.id;
    }

    public static UUID randomId() {
        return UUID.randomUUID();
    }

    public EventFuture readSubscription(UUID id) throws Exception {
        Event event = client.readZNodeData(getEventPath(id), Event.class);
        return new EventFuture(event, this.client);
    }

    private static String getEventPath(UUID id) {
        return ZookeeperPaths.ASYNC_REQUESTS + '/' + id;
    }
}
