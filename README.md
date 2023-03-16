# keycloak-event-listener-mqtt

A Keycloak SPI that publishes events to a MQTT broker.

This SPI has been deployed successfully on a containerized Keycloak 19.0.2
and on a Keycloak 19.0 server on a kubernetes cluster. It should therefore
work properly on any version of Keycloak above 19.0.2.

# Build

```
mvn clean install
```

# Deploy

## Keycloak on Wildfly

* Copy target/event-listener-mqtt-jar-with-dependencies.jar to {KEYCLOAK_HOME}/standalone/deployments
* Edit standalone.xml to configure the MQTT service settings. Find the following
  section in the configuration:

```
<subsystem xmlns="urn:jboss:domain:keycloak-server:1.1">
    <web-context>auth</web-context>
```

And add below:

```
<spi name="eventsListener">
    <provider name="mqtt" enabled="true">
        <properties>
            <property name="serverUri" value="tcp://127.0.0.1:1883"/>
            <property name="username" value="mqtt_user"/>
            <property name="password" value="mqtt_password"/>
            <property name="topic" value="my_topic"/>
            <property name="usePersistence" value="true">
            <property name="retained" value="true">
            <property name="cleanSession" value="true">
            <property name="qos" value="0">
        </properties>
    </provider>
</spi>
```
Leave username and password out if the service allows anonymous write access.
If unset, the default message topic is "keycloak/events".
By default, the SPI won't use persistence. If set to true, messages will be persisted in memory.

* Restart the keycloak server.

## Keycloak on Quarkus

* Copy the jar archive to /opt/keycloak/providers/ in the keycloak container.
* run keycloak with the following options:

```
kc.sh start
  --spi-events-listener-mqtt-server-uri "tcp://your.mqtt.server:port" \
  --spi-events-listener-mqtt-username mqtt_user \
  --spi-events-listener-mqtt-password mqtt_password \
  --spi-events-listener-mqtt-topic my_topic
  --spi-events-listener-mqtt-use-persistence true
  --spi-events-listener-mqtt-retained true
  --spi-events-listener-mqtt-clean-session true
  --spi-events-listener-mqtt-qos 0
```

# Trying it out

The Dockerfile in the `testing` directory can be used to build a keycloak container image
with the listener pre-installed. It assumes the compiled jar has been generated.

The compose in the same directory will launch keycloak and a MQTT server; keycloak is configured
to publish to this server - however the listener must be enabled on any realm.

The `demo.sh` script at the root of the repository automates all the steps above up to and
including configuring the master realm to publish events to the MQTT server, and can be used
to test the event listener out.