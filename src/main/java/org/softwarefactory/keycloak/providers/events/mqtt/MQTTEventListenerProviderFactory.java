/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.softwarefactory.keycloak.providers.events.mqtt;

import java.util.HashSet;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.softwarefactory.keycloak.providers.events.models.Configuration;

/**
 * @author <a href="mailto:mhuin@redhat.com">Matthieu Huin</a>
 */
public class MQTTEventListenerProviderFactory implements EventListenerProviderFactory {

    private Configuration configuration;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new MQTTEventListenerProvider(configuration);
    }

    @Override
    public void init(Config.Scope config) {
        configuration = new Configuration();
        String[] excludes = config.getArray("exclude-events");
        if (excludes != null) {
            configuration.excludedEvents = new HashSet<>();
            for (String e : excludes) {
                configuration.excludedEvents.add(EventType.valueOf(e));
            }
        }

        String[] excludesOperations = config.getArray("excludesOperations");
        if (excludesOperations != null) {
            configuration.excludedAdminOperations = new HashSet<>();
            for (String e : excludesOperations) {
                configuration.excludedAdminOperations.add(OperationType.valueOf(e));
            }
        }

        configuration.serverUri = config.get("serverUri", "tcp://localhost:1883");
        configuration.username = config.get("username", null);
        configuration.password = config.get("password", null);
        configuration.topic = config.get("topic", "keycloak/events");
        configuration.usePersistence = config.getBoolean("usePersistence", false);
        configuration.retained = config.getBoolean("retained", true);
        configuration.cleanSession = config.getBoolean("cleanSession", true);
        configuration.qos = config.getInt("qos", 0);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // not needed
    }

    @Override
    public void close() {
        // not needed
    }

    @Override
    public String getId() {
        return "mqtt";
    }
}