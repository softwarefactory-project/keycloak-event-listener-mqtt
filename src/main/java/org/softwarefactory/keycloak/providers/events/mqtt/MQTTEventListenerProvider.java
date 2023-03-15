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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.softwarefactory.keycloak.providers.events.models.Configuration;

/**
 * @author <a href="mailto:mhuin@redhat.com">Matthieu Huin</a>
 */
public class MQTTEventListenerProvider implements EventListenerProvider {
    private static final Logger logger = Logger.getLogger(MQTTEventListenerProvider.class.getName());

    private Configuration configuration;
    public static final String PUBLISHER_ID = "keycloak";

    public MQTTEventListenerProvider(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onEvent(Event event) {
        // Ignore excluded events
        if (configuration.excludedEvents == null || !this.configuration.excludedEvents.contains(event.getType())) {
            sendMqttMessage(convertEvent(event));
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Ignore excluded operations
        if (configuration.excludedAdminOperations == null || !configuration.excludedAdminOperations.contains(event.getOperationType())) {
            sendMqttMessage(convertAdminEvent(event));
        }
    }

    private void sendMqttMessage(String event) {
        MemoryPersistence persistence = null;
        if (configuration.usePersistence) {
            persistence = new MemoryPersistence();
        }

        try (IMqttClient client = new MqttClient(configuration.serverUri, PUBLISHER_ID, persistence)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(configuration.cleanSession);
            options.setConnectionTimeout(10);

            if (configuration.username != null && configuration.password != null) {
                options.setUserName(configuration.username);
                options.setPassword(configuration.password.toCharArray());
            }

            client.connect(options);
            logger.log(Level.FINE, "Event: {0}", event);
            MqttMessage payload = toPayload(event);
            payload.setQos(configuration.qos);
            payload.setRetained(configuration.retained);
            client.publish(configuration.topic, payload);
            client.disconnect();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Event: {0}", e.getStackTrace());
        }
    }

    private MqttMessage toPayload(String s) {
        byte[] payload = s.getBytes();
        return new MqttMessage(payload);
    }

    private String convertEvent(Event event) {
        JSONObject ev = new JSONObject();

        ev.put("type", event.getType().toString());
        ev.put("realmId", event.getRealmId());
        ev.put("clientId", event.getClientId());
        ev.put("userId", event.getUserId());
        ev.put("ipAddress", event.getIpAddress());
        ev.put("time", event.getTime());

        ev.put("error", event.getError());

        JSONObject evDetails = new JSONObject();
        if (event.getDetails() != null) {
            for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
                evDetails.put(e.getKey(), e.getValue());
            }
        }
        ev.put("details", evDetails);

        return ev.toString();
    }

    private String convertAdminEvent(AdminEvent adminEvent) {
        JSONObject ev = new JSONObject();

        ev.put("type", adminEvent.getOperationType().toString());
        ev.put("realmId", adminEvent.getAuthDetails().getRealmId());
        ev.put("clientId", adminEvent.getAuthDetails().getClientId());
        ev.put("userId", adminEvent.getAuthDetails().getUserId());
        ev.put("ipAddress", adminEvent.getAuthDetails().getIpAddress());
        ev.put("time", adminEvent.getTime());
        ev.put("resourcePath", adminEvent.getResourcePath());
        ev.put("resourceType", adminEvent.getResourceTypeAsString());

        ev.put("error", adminEvent.getError());

        return ev.toString();
    }

    @Override
    public void close() {
    }

}
