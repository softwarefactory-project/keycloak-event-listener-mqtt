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
import java.util.Set;
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
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;

/**
 * @author <a href="mailto:mhuin@redhat.com">Matthieu Huin</a>
 */
public class MQTTEventListenerProvider implements EventListenerProvider {
    private static final Logger logger = Logger.getLogger(MQTTEventListenerProvider.class.getName());

    private Set<EventType> excludedEvents;
    private Set<OperationType> excludedAdminOperations;
    private String serverUri;
    private String username;
    private String password;
    private String topic;
    private boolean usePersistence;
    public static final String PUBLISHER_ID = "keycloak";

    public MQTTEventListenerProvider(Set<EventType> excludedEvents, Set<OperationType> excludedAdminOperations,
            String serverUri, String username, String password, String topic, boolean usePersistence) {
        this.excludedEvents = excludedEvents;
        this.excludedAdminOperations = excludedAdminOperations;
        this.serverUri = serverUri;
        this.username = username;
        this.password = password;
        this.topic = topic;
        this.usePersistence = usePersistence;
    }

    @Override
    public void onEvent(Event event) {
        // Ignore excluded events
        if (excludedEvents == null || !excludedEvents.contains(event.getType())) {
            sendMqttMessage(convertEvent(event));
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Ignore excluded operations
        if (excludedAdminOperations == null || !excludedAdminOperations.contains(event.getOperationType())) {
            sendMqttMessage(convertAdminEvent(event));
        }
    }

    private void sendMqttMessage(String event) {
        MemoryPersistence persistence = null;
        if (this.usePersistence) {
            persistence = new MemoryPersistence();
        }

        try (IMqttClient client = new MqttClient(this.serverUri, PUBLISHER_ID, persistence)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            if (this.username != null && this.password != null) {
                options.setUserName(this.username);
                options.setPassword(this.password.toCharArray());
            }

            client.connect(options);
            logger.log(Level.FINE, "Event: {0}", event);
            MqttMessage payload = toPayload(event);
            payload.setQos(0);
            payload.setRetained(true);
            client.publish(this.topic, payload);
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
