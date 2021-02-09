package com.qu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.services.queue.event.QueueEventHandlersImpl;
import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import com.qu.test.utils.DaoUtil;
import com.qu.test.utils.Sql;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.common.mapper.TypeRef;
import io.smallrye.common.annotation.Blocking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;

import java.util.List;
import java.util.Set;

import static com.qu.test.utils.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static com.qu.test.utils.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static com.qu.test.utils.TestUtils.readTestResourceAsString;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@Blocking       //we need this to run jdbi with jdbc database connection for some reason
public class QueueMgrApiTest {

    private static final String DUMMY_HANDLER = "HANDLER";
    @Inject
    DaoUtil dao;

    @Inject
    ObjectMapper mapper;

    static String adminJwt = readTestResourceAsString("keys/admin.jwt");

    @InjectMock
    QueueEventHandlersImpl handlers;

    @BeforeEach
    public void init(){
        var mockSet = Set.<QueueEventHandlerInfo<?>>of(getTestEventHandler());
        Mockito.when(handlers.getActiveHandlers()).thenReturn(mockSet);
    }


    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/organization_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void getEventsList(){
        var response =
                given()
                    .when()
                        .auth().oauth2(adminJwt)
                    .get("/queue/event/handlers")
                    .then()
                    .statusCode(204)
                    .body(notNullValue())
                    .extract()
                    .body()
                    .as(new TypeRef<List<QueueEventHandlerInfo<String>>>(){});

        assertEquals(1, response.size());
    }




    private QueueEventHandlerInfo<?> getTestEventHandler() {
        var handler = new QueueEventHandlerInfo<String>();
        handler.name = DUMMY_HANDLER;
        handler.description = "dummy";
        handler.parameterClass = String.class;
        handler.parameterDefaults = "No!";
        return handler;
    }
}




