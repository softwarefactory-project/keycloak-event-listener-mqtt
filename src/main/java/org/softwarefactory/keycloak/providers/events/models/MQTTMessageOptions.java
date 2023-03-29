package org.softwarefactory.keycloak.providers.events.models;

public class MQTTMessageOptions {
  public boolean retained;
  public boolean cleanSession;
  public int qos;
  public String topic;
}
