package com.qu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.dto.AdminInvitationCreateResponse;
import com.qu.dto.AdminInvitationDTO;
import com.qu.dto.UserCreationDto;
import com.qu.persistence.entities.Organization;
import com.qu.services.KeycloakService;
import com.qu.test.dto.AdminInvitationRow;
import com.qu.test.utils.DaoUtil;
import com.qu.test.utils.Sql;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.common.mapper.TypeRef;
import io.smallrye.common.annotation.Blocking;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.qu.commons.constants.Roles.QUEUE_ADMIN;
import static com.qu.commons.constants.Urls.ADMIN_INVITATION_FORM;
import static com.qu.commons.enums.UserGroup.OWNER;
import static com.qu.test.utils.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static com.qu.test.utils.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static com.qu.test.utils.TestUtils.readTestResourceAsString;
import static io.restassured.RestAssured.given;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

@QuarkusTest
@Blocking       //we need this to run jdbi with jdbc database connection for some reason
//@QuarkusTestResource(OidcWiremockTestResource.class)
public class OrganizationApiTest {

    @Inject
    DaoUtil dao;

    @Inject
    ObjectMapper mapper;

    @Inject
    MockMailbox mailbox;

    @InjectMock
    KeycloakService keycloakService;


    static String jwt = readTestResourceAsString("keys/owner.jwt");


    @BeforeEach
    void init() {
        mailbox.clear();
        mockKeycloakService();
    }


    private void mockKeycloakService() {
        Mockito.when(keycloakService.createKeycloakUser(any(), any()))
                .thenReturn("MOCKED_USER_ID");
        Mockito.when(keycloakService.createOrgRole(any()))
                .thenAnswer(an -> {
                    var orgRoleName = "ORG_"+ an.getArgument(0, Long.class);
                    var orgRole = new RoleRepresentation();
                    orgRole.setName(orgRoleName);
                    return orgRole; });
        Mockito.doNothing().when(keycloakService).addRolesToKeycloakUser(Mockito.anyList(), any());
    }


    @Test
    public void registerOrganization(){
        String email = "owner@fake.com";
        String password = "1234";
        String name = "organization";
        String paymentToken = "we_payed";
        String request =
                createObjectBuilder()
                .add("email", email)
                .add("password", password)
                .add("name", name)
                .add("payment_token", paymentToken)
                .build()
                .toString();
        var id =
            given()
            .when()
            .body(request)
                    .contentType(APPLICATION_JSON.toString())
            .post("/organization")
            .then()
            .statusCode(200)
            .body(notNullValue())
            .extract().body().asString();

        var org = dao.getSingleRow("select * from organization where id = :id", Organization.class, Map.of("id", Long.parseLong(id)));

        assertNotNull(email, org.getOwnerId());
        assertEquals(name, org.getName());
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/organization_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void inviteOrganizationAdmin(){
        var email = "admin@fake.com";
        var roles = createArrayBuilder().add(QUEUE_ADMIN).build();
        var request =
                createObjectBuilder()
                .add("email", email)
                .add("roles", roles)
                .build()
                .toString();
        var response =
                given()
                    .when()
                        .body(request)
                            .contentType(APPLICATION_JSON.toString())
                            .auth().oauth2(jwt)
                        .post("/organization/admin/invitation")
                    .then()
                        .statusCode(200)
                        .body(notNullValue())
                        .extract()
                            .body().as(AdminInvitationCreateResponse.class);

        var invitation = dao.getSingleRow("select * from organization_admin_invitation where id = :id", AdminInvitationRow.class, Map.of("id", response.invitationToken));

        assertNotNull(invitation.getId());
        assertEquals(8888L, invitation.getOrganizationId());
        assertEquals(email, invitation.getEmail());
        assertEquals(roles.toString(), invitation.getRoles());

        var msgs = ofNullable(mailbox.getMessagesSentTo(email)).orElse(emptyList());
        assertEquals( 1, msgs.size());
        String mailBody = msgs.get(0).getHtml();
        assertNotNull(mailBody);
    }






    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/organization_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void acceptAdminInvitation(){
        var email = "admin@fake.com";

        var regPage =
                given()
                    .when()
                    .param("token", "token")
                    .get("/organization"+ADMIN_INVITATION_FORM)
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .extract()
                    .body()
                    .asPrettyString();

        var regResponse =
                given()
                    .when()
                    .formParam("token", "token")
                        .formParam("password", "p@ss")
                        .formParam("passwordRepeat", "p@ss")
                    .post("/organization"+ADMIN_INVITATION_FORM)
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .extract()
                    .body()
                    .asPrettyString();

        assertTrue(true);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/organization_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void acceptAdminInvitationFail(){
        var regResponse =
                given()
                        .when()
                        .formParam("token", "token")
                        .formParam("password", "p@ss")
                        .formParam("passwordRepeat", "NOTp@ss")
                        .post("/organization"+ADMIN_INVITATION_FORM)
                        .then()
                        .statusCode(200)
                        .body(notNullValue())
                        .extract()
                        .body()
                        .asPrettyString();

        assertTrue(true);
    }





    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/organization_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void listAdminInvitations(){
        var response =
                given()
                    .when()
                        .auth().oauth2(jwt)
                    .get("/organization/admin/invitation")
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .extract()
                    .body()
                    .as(new TypeRef<List<AdminInvitationDTO>>(){ });

        assertEquals(1, response.size());
        var invitation = response.get(0);
        assertEquals("token", invitation.id);
        assertEquals("admin@fake.com", invitation.email);
        assertEquals(Set.of("QUEUE_MANAGER"), invitation.roles);
        assertNotNull(invitation.creationTime);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/organization_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void cancelAdminInvitations(){
        var countBefore = dao.getSingleResult("select count(*) as cnt from organization_admin_invitation where id = :id", Integer.class, Map.of("id", "token"));
        assertEquals(1, countBefore);

        given()
            .when()
            .auth().oauth2(jwt)
            .delete("/organization/admin/invitation/token")
            .then()
            .statusCode(204);

        var countAfter = dao.getSingleResult("select count(*) from organization_admin_invitation where id = :id", Long.class, Map.of("id", "token"));
        assertEquals(0L, countAfter);
    }

}




