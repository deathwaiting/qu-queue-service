package com.qu;

import com.qu.persistence.entities.Organization;
import com.qu.test.utils.DaoUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import org.apache.http.entity.ContentType;
import org.hamcrest.CoreMatchers;
import org.hibernate.Session;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.json.Json;
import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class OrganizationApiTest {

    @Inject
    DaoUtil dao;

    @Test
    @Blocking
    public void registerOrganization(){
        String email = "owner@fake.com";
        String password = "1234";
        String name = "organization";
        String paymentToken = "we_payed";
        String request =
                Json
                .createObjectBuilder()
                .add("email", email)
                .add("password", password)
                .add("name", name)
                .add("payment_token", paymentToken)
                .build()
                .toString();
        Long id =
            given()
            .when()
            .body(request)
                    .contentType(APPLICATION_JSON.toString())
            .post("/organization")
            .then()
            .statusCode(200)
            .body(notNullValue())
            .extract().body().as(Long.class);

        var org = dao.getSingleRow("select * from organization where id = :id", Organization.class, Map.of("id", id));

        assertNotNull(email, org.getOwnerId());
        assertEquals(name, org.getName());
    }
}
