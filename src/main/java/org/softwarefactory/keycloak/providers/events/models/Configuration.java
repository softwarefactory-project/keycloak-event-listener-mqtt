package org.softwarefactory.keycloak.providers.events.models;

import java.util.Set;

import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;

public class Configuration {
  public Set<EventType> excludedEvents;
  public Set<OperationType> excludedAdminOperations;
  public String serverUri;
  public String username;
  public String password;
  public String topic;
  public boolean usePersistence;
  public boolean retained;
  public boolean cleanSession;
  public int qos;
}
