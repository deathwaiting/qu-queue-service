package com.qu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.dto.*;
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
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

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
                dao.runQuery("select * from queue_event_handler where queue_type_id = :id"
                        , QueueMgrApiTest.EventHandlerRow.class
                        , Map.of("id", response));

        assertEquals(name, newTemplate.name);
        assertEquals(1, newEventsHandlers.size());
        assertEquals(DUMMY_HANDLER, newEventsHandlers.get(0).name);
        assertEquals(ENQUEUE_ACTION.name(), newEventsHandlers.get(0).eventType);
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



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void getQueuesDetails(){
        var response =
                given()
                        .when()
                        .auth().oauth2(serverJwt)
                        .get("/queue/99934")
                        .then()
                        .statusCode(200)
                        .body(notNullValue())
                        .extract()
                        .body()
                        .as(QueueDetailsDto.class);

        assertEquals(3, response.turns.size());
        var expectedTurns = List.of("ABC123", "ABC125", "ABC124");
        var returnedTurns = response.turns.stream().map(QueueTurnDto::getNumber).collect(toList());
        assertEquals(expectedTurns, returnedTurns);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void addQueueAction(){
        var id = 99934L;
        var response =
                given()
                    .when()
                    .contentType(JSON)
                    .auth().oauth2(adminJwt)
                    .post("/queue/{id}/action?action=END", id)
                    .then()
                    .statusCode(204);

        var row =
                dao
                    .getFirstRow("SELECT * FROM QUEUE_ACTIONS" +
                                    " WHERE queue_id = :id" +
                                    " order by action_time desc"
                            , QueueActionRow.class
                            , Map.of("id", id));
        assertNotNull(row.actionTime);
        assertNotNull(row.id);
        assertEquals(id, row.queueId);
        assertEquals("END", row.actionType);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void addTurn(){
        var id = 99934L;
        var body = createEnqueueRequestBody();
        var response =
                given()
                    .when()
                    .contentType(JSON)
                    .auth().oauth2(serverJwt)
                    .body(body)
                    .post("/queue/{id}/turn", id)
                    .then()
                    .statusCode(200);

        var requestRow =
                dao
                .getFirstRow("SELECT * FROM QUEUE_REQUEST" +
                                " WHERE queue_id = :id" +
                                " order by request_Time desc"
                        , QueueRequestRow.class
                        , Map.of("id", id));
        assertNotNull(requestRow.requestTime);
        assertNotNull(requestRow.responseTime);
        assertNotNull(requestRow.id);
        assertEquals(id, requestRow.queueId);

        var turnRow =
                dao
                    .getFirstRow("SELECT id, enqueue_time, request_id, queue_number FROM QUEUE_TURN" +
                                    " WHERE request_id = :id" +
                                    " order by enqueue_time desc"
                            , QueueTurnRow.class
                            , Map.of("id", requestRow.id));
        assertNotNull(turnRow.enqueueTime);
        assertEquals("1", turnRow.queueNumber);
    }



    private String createEnqueueRequestBody() {
        return createObjectBuilder()
                .add("user_id", "dummy_user")
                .add("user_details",
                        createObjectBuilder()
                            .add("email", "dummy@fake.com")
                            .add("magic_num", 1111)
                            .build())
                .build()
                .toString();
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


    @Data
    public static class QueueActionRow{
        public Long id;
        public String actionType;
        public Long queueId;
        public LocalDateTime actionTime;
    }


    @Data
    public static class QueueRequestRow{
        public Long id;
        public String clientId;
        public LocalDateTime requestTime;
        public LocalDateTime responseTime;
        public LocalDateTime skipTime;
        public String clientDetails;
        public Long queueId;
        public Boolean refused;
        public String acceptorId;
    }



    @Data
    public static class QueueTurnRow{
        public Long id;
        public LocalDateTime enqueueTime;
        public Long requestId;
        public String queueNumber;
    }
}




