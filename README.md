# keycloak-event-listener-mqtt

A Keycloak SPI that publishes events to a MQTT broker.

# Build

```
mvn clean install
```

# Deploy

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
        </properties>
    </provider>
</spi>
```
Leave username and password out if the service allows anonymous write access.
If unset, the default message topic is "keycloak/events".

* Restart the keycloak server.
