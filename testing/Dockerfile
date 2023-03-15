ARG KEYCLOAK_VERSION=21.0

FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION as builder

# Install built provider - this assumes "maven clean build" was run before
COPY ./target/*-with-dependencies.jar /opt/keycloak/providers/

USER 1000

RUN /opt/keycloak/bin/kc.sh build --health-enabled=true

FROM quay.io/keycloak/keycloak:$KEYCLOAK_VERSION
COPY --from=builder /opt/keycloak/ /opt/keycloak/
WORKDIR /opt/keycloak


ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]