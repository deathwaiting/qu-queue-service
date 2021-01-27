package com.qu.test.resources;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.Map;

import static java.util.Collections.emptyMap;


/**
 * a test resource for running a postgres database container.
 * unfortunately , Testcontainer library needs docker to be installed, and it can't use podman as it
 * needs specific features from docker that podman doesn't provide
 * */
public class PostgresDatabaseContainer implements QuarkusTestResourceLifecycleManager {
    private final GenericContainer postgresContainer =
            new PostgreSQLContainer(DockerImageName.parse("postgres"))
                .withExposedPorts(5432);


    @Override
    public Map<String, String> start() {
        postgresContainer.start();
        return emptyMap();
    }



    @Override
    public void stop() {
         postgresContainer.close();
    }
}
