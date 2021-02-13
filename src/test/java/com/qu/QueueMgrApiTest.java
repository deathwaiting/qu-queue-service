package com.qu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.dto.QueueDto;
import com.qu.dto.QueueListResponse;
import com.qu.dto.QueueTypeDto;
import com.qu.services.queue.event.QueueEventHandlersImpl;
import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import com.qu.test.utils.DaoUtil;
import com.qu.test.utils.Sql;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.common.mapper.TypeRef;
import io.smallrye.common.annotation.Blocking;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.qu.services.QueueEventPhase.ENQUEUE_ACTION;
import static com.qu.test.utils.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static com.qu.test.utils.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static com.qu.test.utils.TestUtils.readTestResourceAsString;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@Blocking       //we need this to run jdbi with jdbc database connection for some reason
public class QueueMgrApiTest {

    private static final String DUMMY_HANDLER = "HANDLER";
    @Inject
    DaoUtil dao;

    @Inject
    ObjectMapper mapper;

    static String adminJwt = readTestResourceAsString("keys/admin.jwt");
    static String serverJwt = readTestResourceAsString("keys/server.jwt");

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
                    .statusCode(200)
                    .body(notNullValue())
                    .extract()
                    .body()
                    .as(new TypeRef<List<QueueEventHandlerInfo<String>>>(){});

        assertEquals(1, response.size());
        assertEquals(getTestEventHandler(), response.get(0));
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/organization_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void createQueueTemplate(){
        var name = "New queue Template!";
        var body = createQueueTypeCreationRequest(name);

        var response =
                given()
                    .when()
                    .contentType(JSON)
                    .auth().oauth2(adminJwt)
                    .body(body)
                    .post("/queue/type")
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .extract()
                    .body()
                    .as(Long.class);

        var newTemplate =
                dao.getSingleRow("select * from queue_type where id = :id"
                        , QueueMgrApiTest.QueTemplateRow.class
                        , Map.of("id", response));

        var newEventsHandlers =
                dao.runQuery("select * from queue_event_definition where queue_type_id = :id"
                        , QueueMgrApiTest.EventHandlerRow.class
                        , Map.of("id", response));

        assertEquals(name, newTemplate.name);
        assertEquals(1, newEventsHandlers.size());
        assertEquals(DUMMY_HANDLER, newEventsHandlers.get(0).name);
        assertEquals(ENQUEUE_ACTION.name(), newEventsHandlers.get(0).eventData);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void getQueueTypesList(){
        var response =
                given()
                    .when()
                    .auth().oauth2(adminJwt)
                    .get("/queue/type")
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .extract()
                    .body()
                    .as(new TypeRef<List<QueueTypeDto>>(){});

        assertEquals(1, response.size());
        assertEquals(2, response.get(0).eventHandlers.size());
    }




    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void createQueue(){
        var body = createQueueRequest();
        var response =
                given()
                    .when()
                    .contentType(JSON)
                    .auth().oauth2(adminJwt)
                    .body(body)
                    .post("/queue")
                    .then()
                    .statusCode(200)
                    .body(notNullValue())
                    .extract()
                    .body()
                    .as(Long.class);

        var row =
                dao
                .getSingleRow("SELECT * FROM QUEUE WHERE id = :id"
                        , QueueRow.class
                        , Map.of("id", response));

        //times are saved in UTC
        assertEquals(LocalDateTime.of(1994,11,5,6,15,30) , row.startTime);
        assertEquals(LocalDateTime.of(1994,11,5,11,15,30) , row.endTime);
        assertEquals(row.maxSize, 10);
        assertTrue(row.autoAcceptEnabled && row.holdEnabled);
    }




    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void getQueuesList(){
        var response =
                given()
                        .when()
                            .auth().oauth2(serverJwt)
                        .get("/queue")
                        .then()
                        .statusCode(200)
                        .body(notNullValue())
                        .extract()
                        .body()
                        .as(QueueListResponse.class);

        assertEquals(2, response.queues.size());
    }



    private String createQueueRequest() {
        return createObjectBuilder()
                .add("autoAcceptEnabled", true)
                .add("queue_type_id", 88888)
                .add("holdEnabled", true)
                .add("max_size", 10)
                .add("start_time", "1994-11-05T08:15:30+02:00")
                .add("end_time", "1994-11-05T13:15:30+02:00")
                .build()
                .toString();
    }


    private String createQueueTypeCreationRequest(String name) {
        var handler =
                createObjectBuilder()
                    .add("event_handler_name", DUMMY_HANDLER)
                    .add("common_params", createObjectBuilder().add("pokemon", "picktchu").build())
                    .add("type", ENQUEUE_ACTION.name())
                .build();
        var eventHandlers =
                createArrayBuilder()
                        .add( handler)
                        .build();
        return  createObjectBuilder()
                .add("name", name)
                .add("default_max_size", 50)
                        .add("event_handlers", eventHandlers)
                .build()
                .toString();
    }



    private QueueEventHandlerInfo<?> getTestEventHandler() {
        var handler = new QueueEventHandlerInfo<String>();
        handler.name = DUMMY_HANDLER;
        handler.description = "dummy";
        handler.parameterClass = String.class;
        handler.parameterDefaults = "No!";
        handler.eventPhase = ENQUEUE_ACTION;
        return handler;
    }



    @Data
     public static class QueTemplateRow{
        public Long id;
        public boolean defaultAutoAcceptEnabled;
        public boolean defaultHoldEnabled;
        public Integer defaultMaxSize;
        public Long organizationId;
        public String name;
    }


    @Data
    public static class EventHandlerRow{
        public Long id;
        public String eventData;
        public String eventType;
        public String name;
        public Long queueTypeId;
    }



    @Data
    public static class QueueRow{
        public Long id;
        public Boolean autoAcceptEnabled;
        public Boolean holdEnabled;
        public Integer maxSize;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
    }
}




