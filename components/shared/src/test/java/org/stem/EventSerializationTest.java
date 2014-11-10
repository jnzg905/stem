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

package org.stem;

import org.junit.Test;
import org.stem.api.response.StemResponse;
import org.stem.coordination.Event;
import org.stem.utils.JsonUtils;

public class EventSerializationTest {

    @Test
    public void serialization() throws Exception {
        Event event = Event.create(Event.Type.JOIN);
        StubResponse resp = new StubResponse();
        event.setResponse(resp);

        JsonUtils.encode(event);

    }

    static class StubResponse extends StemResponse {
        public static String a = "test";

        String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public StubResponse() {
        }
    }
}
