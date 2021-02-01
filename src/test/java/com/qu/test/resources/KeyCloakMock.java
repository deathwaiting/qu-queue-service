package com.qu.test.resources;

import com.tngtech.keycloakmock.api.KeycloakMock;
import com.tngtech.keycloakmock.api.ServerConfig;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Collections;
import java.util.Map;

import static com.tngtech.keycloakmock.api.ServerConfig.aServerConfig;
import static java.util.Collections.emptyMap;


/**
 * Keycloak-mock is a mock server for keycloak: https://github.com/TNG/keycloak-mock
 * I tried using it to mock OIDC security, but unfortunately the mock server uses vert.x and it seems there's a
 * dependency conflict for vert.x with quarkus(keycloak uses version 4.0.0 and quarkus seems to use 3.9.5).
 * Anyway ,it didn't work, with the current build, the mock server runs and takes a port but doesn't respond.
 * I just used another way to mock the OIDC server, by setting the property 'quarkus.oidc.public-key' to a public key
 * string and creating a custom JWT with the private key, quarkus will use the JWT for authorization and authentication
 * without contacting the OIDC server.
 * */
public class KeyCloakMock implements QuarkusTestResourceLifecycleManager {

    private KeycloakMock mock;

    @Override
    public Map<String, String> start() {
        mock = new KeycloakMock(aServerConfig().withPort(8180).withRealm("qu").build());
        mock.start();
        return emptyMap();
    }


    @Override
    public void stop() {
        mock.stop();
    }
}
