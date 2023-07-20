#!/bin/sh

# Set up keycloak and a mqtt service to test the event listener manually.
# Set env variable KEYCLOAK_VERSION to any version you would like to
# test the listener against (it must be Quarkus-based so > 17)

# TODO support docker and choose the runtime to use automatically

echo "Building event listener provider ..."
mvn clean install
echo "Done."
echo

echo "Building keycloak with event listener ..."
podman build -t test_kc_event_listener --build-arg KEYCLOAK_VERSION=${KEYCLOAK_VERSION:-22.0} -f testing/Dockerfile .
echo "Done."
echo

echo "Starting compose ..."
podman-compose -f testing/docker-compose.yaml up -d
echo
echo "Waiting for keycloak to start ..."
until curl -s -f -o /dev/null http://localhost:8082/health/ready
do
  echo "."
  sleep 5
done
echo "Ready."

echo "Configuring mqtt event listener on master realm ..."
podman exec -ti testing_keycloak_1 /opt/keycloak/bin/kcadm.sh update events/config \
  --target-realm master --set 'eventsListeners=["jboss-logging", "mqtt"]' \
  --set eventsEnabled=true --set enabledEventTypes=[] \
  --no-config --user admin --realm master --server http://localhost:8082 --password kcadmin
echo "Done."
echo


echo "To subscribe to published events:"
echo "  mosquitto_sub -h localhost -u mqtt -P mqtt -t 'keycloak'"
echo
echo "To trigger a login event:"
echo "  podman exec -ti testing_keycloak_1 /opt/keycloak/bin/kcadm.sh get users --target-realm master --no-config --user admin --realm master --server http://localhost:8082 --password kcadmin"
echo
echo "To kill the compose:"
echo "  podman-compose -f testing/docker-compose.yaml down"