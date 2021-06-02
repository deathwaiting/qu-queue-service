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
import io.restassured.response.ValidatableResponse;
import io.smallrye.common.annotation.Blocking;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.json.JsonObject;

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
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
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

        assertTurnCreated(requestRow);
    }



    private void assertTurnCreated(QueueRequestRow requestRow) {
        assertTurnCreated(requestRow.id);
    }



    private void assertTurnCreated(Long requestId) {
        var turnRow =
                dao
                .getFirstRow("SELECT id, enqueue_time, request_id, queue_number FROM QUEUE_TURN" +
                                " WHERE request_id = :id" +
                                " order by enqueue_time desc"
                        , QueueTurnRow.class
                        , Map.of("id", requestId));
        assertNotNull(turnRow.enqueueTime);
        assertEquals("1", turnRow.queueNumber);
    }



    private void assertHasSingleTurnCreated(Long requestId) {
        assertNTurnCreated(1L,requestId);
    }



    private void assertNoTurnCreated(Long requestId) {
        assertNTurnCreated(0L,requestId);
    }



    private void assertNTurnCreated(Long expectedCount, Long requestId) {
        var count =
                dao
                .getSingleResult("SELECT count(*) FROM QUEUE_TURN" +
                                " WHERE request_id = :id"
                        , Long.class
                        , Map.of("id", requestId));
        assertEquals(expectedCount, count);
    }


    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void makeRequestAsAnonymus(){
        var id = 99934L;
        var clientDetails =
                createObjectBuilder()
                .add("name", "El za3eem 3del shakal")
                .add("email", "el.na7o.545@shakal.com")
                .build();
        var body = createTurnRequestRequestBody(clientDetails);
        var response =
                postTurnRequestToApp(id, body)
                        .statusCode(200);

        assertRequestCreated(id, clientDetails, false);
    }



    private ValidatableResponse postTurnRequestToApp(long id, String body) {
        return given()
                .when()
                .contentType(JSON)
                .body(body)
                .post("/queue/{id}/turn/request", id)
                .then();
    }


    private ValidatableResponse postTurnRequestAcceptToApp(long quId, long requestId) {
        return given()
                .when()
                    .auth().oauth2(serverJwt)
                .contentType(JSON)
                .post("/queue/{id}/turn/request/{request}/accept", quId, requestId)
                .then();
    }


    private ValidatableResponse postTurnRequestDenyToApp(long quId, long requestId) {
        return given()
                .when()
                .auth().oauth2(serverJwt)
                .contentType(JSON)
                .post("/queue/{id}/turn/request/{request}/deny", quId, requestId)
                .then();
    }



    private ValidatableResponse dequeueNextTurn(long quId) {
        return given()
                .when()
                    .auth().oauth2(serverJwt)
                .contentType(JSON)
                .get("/queue/{id}/dequeue", quId)
                .then();
    }


    private ValidatableResponse skipNextTurn(long quId, String reason) {
        return given()
                .when()
                .auth().oauth2(serverJwt)
                .contentType(JSON)
                .post("/queue/{id}/skip?reason={reason}", quId, reason)
                .then();
    }


    private ValidatableResponse cancelTurn(long quId, long turnId) {
        return given()
                .when()
                    .auth().oauth2(serverJwt)
                .contentType(JSON)
                .delete("/queue/{id}/turn/{turn}", quId, turnId)
                .then();
    }


    private ValidatableResponse cancelTurnByCustomer(long quId, long turnId, String clientId) {
        return given()
                .when()
                .contentType(JSON)
                .delete("/queue/{id}/turn/{turn}/by_customer?client_id={clientId}", quId, turnId, clientId)
                .then();
    }


    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void makeRequestAsNonAnonymus(){
        var id = 99934L;
        var clientId = "01111111111";
        var clientDetails =
                createObjectBuilder()
                        .add("name", "El za3eem 3del shakal")
                        .add("email", "el.na7o.545@shakal.com")
                        .build();
        var body = createTurnRequestRequestBody(clientId, clientDetails);
        var response =
                postTurnRequestToApp(id, body)
                    .statusCode(200);

        var requestRow = assertRequestCreated(id, clientDetails, false);
        assertEquals(clientId, requestRow.clientId);
    }


    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_2.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void makeRequestForAutoAcceptQueue(){
        var id = 99937L;
        var clientDetails =
                createObjectBuilder()
                        .add("name", "El za3eem 3del shakal")
                        .add("email", "el.na7o.545@shakal.com")
                        .build();
        var body = createTurnRequestRequestBody(clientDetails);
        var response =
                postTurnRequestToApp(id, body)
                        .statusCode(200);

        var requestRow = assertRequestCreated(id, clientDetails, true);
        assertTurnCreated(requestRow);
    }



    private QueueRequestRow assertRequestCreated(long id, JsonObject clientDetails, boolean hasResponse) {
        var requestRow =
                dao
                    .getFirstRow("SELECT * FROM QUEUE_REQUEST" +
                                    " WHERE queue_id = :id" +
                                    " order by request_Time desc"
                            , QueueRequestRow.class
                            , Map.of("id", id));
        assertNotNull(requestRow.requestTime);
        assertNotNull(requestRow.id);
        assertEquals(id, requestRow.queueId);
        assertNotNull(requestRow.clientId);
        assertEquals(clientDetails.toString(), requestRow.clientDetails);
        if(hasResponse){
            assertNotNull(requestRow.responseTime);
        }else{
            assertNull(requestRow.responseTime);
        }
        return requestRow;
    }

    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_2.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void makeRequestNotStartedQueue(){
        var id = 99933L;
        var clientId = "01111111111";
        var clientDetails =
                createObjectBuilder()
                        .add("name", "El za3eem 3del shakal")
                        .add("email", "el.na7o.545@shakal.com")
                        .build();
        var body = createTurnRequestRequestBody(clientId, clientDetails);
        var response =
                postTurnRequestToApp(id, body)
                        .statusCode(406);

        var requestRow =
                dao
                .getFirstRow("SELECT * FROM QUEUE_REQUEST" +
                                " WHERE queue_id = :id" +
                                " order by request_Time desc"
                        , QueueRequestRow.class
                        , Map.of("id", id));
        assertNull(requestRow);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_2.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void makeRequestPausedQueue(){
        var id = 99934L;
        var clientId = "01111111111";
        var clientDetails =
                createObjectBuilder()
                        .add("name", "El za3eem 3del shakal")
                        .add("email", "el.na7o.545@shakal.com")
                        .build();
        var body = createTurnRequestRequestBody(clientId, clientDetails);
        var response =
                postTurnRequestToApp(id, body)
                    .statusCode(406);

        var requestRow =
                dao
                .getFirstRow("SELECT * FROM QUEUE_REQUEST" +
                                " WHERE queue_id = :id" +
                                " order by request_Time desc"
                        , QueueRequestRow.class
                        , Map.of("id", id));
        assertNull(requestRow);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_2.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void makeRequestEndedQueue(){
        var id = 99935L;
        var clientId = "01111111111";
        var clientDetails =
                createObjectBuilder()
                        .add("name", "El za3eem 3del shakal")
                        .add("email", "el.na7o.545@shakal.com")
                        .build();
        var body = createTurnRequestRequestBody(clientId, clientDetails);
        var response =
                postTurnRequestToApp(id, body)
                    .statusCode(406);

        var requestRow =
                dao
                .getFirstRow("SELECT * FROM QUEUE_REQUEST" +
                                " WHERE queue_id = :id" +
                                " order by request_Time desc"
                        , QueueRequestRow.class
                        , Map.of("id", id));
        assertNull(requestRow);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_2.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void makeRequestMaxedQueue(){
        var id = 99936L;
        var clientId = "01111111111";

        var countRequestsBefore = countRequests(id);

        var clientDetails =
                createObjectBuilder()
                        .add("name", "El za3eem 3del shakal")
                        .add("email", "el.na7o.545@shakal.com")
                        .build();
        var body = createTurnRequestRequestBody(clientId, clientDetails);
        var response =
                postTurnRequestToApp(id, body)
                    .statusCode(406);

        var countRequestsAfter = countRequests(id);
        assertEquals(countRequestsBefore, countRequestsAfter);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_2.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void acceptRequest(){
        var quId = 99938L;
        var requestId = 98004L;
        var response =
                postTurnRequestAcceptToApp(quId, requestId)
                        .statusCode(200);

        assertTurnCreated(requestId);

        var requestRow =
                dao
                .getFirstRow("SELECT * FROM QUEUE_REQUEST" +
                                " WHERE id = :id" +
                                " order by request_Time desc"
                        , QueueRequestRow.class
                        , Map.of("id", requestId));
        assertNotNull(requestRow.responseTime);
        assertNotNull(requestRow.acceptorId);
        assertFalse(requestRow.refused);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_2.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void acceptHandledRequest(){
        var quId = 99938L;
        var requestId = 98005L;
        assertHasSingleTurnCreated(requestId);
        var response =
                postTurnRequestAcceptToApp(quId, requestId)
                        .statusCode(406);

        assertHasSingleTurnCreated(requestId);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_2.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void denyRequest(){
        var quId = 99938L;
        var requestId = 98004L;
        var response =
                postTurnRequestDenyToApp(quId, requestId)
                        .statusCode(204);

        assertNoTurnCreated(requestId);

        var requestRow =
                dao
                .getFirstRow("SELECT * FROM QUEUE_REQUEST" +
                                " WHERE id = :id" +
                                " order by request_Time desc"
                        , QueueRequestRow.class
                        , Map.of("id", requestId));
        assertNotNull(requestRow.responseTime);
        assertNotNull(requestRow.acceptorId);
        assertTrue(requestRow.refused);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_2.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void denyHandledRequest(){
        var quId = 99938L;
        var requestId = 98005L;
        assertHasSingleTurnCreated(requestId);
        var response =
                postTurnRequestDenyToApp(quId, requestId)
                        .statusCode(406);

        assertHasSingleTurnCreated(requestId);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void dequeueTurn(){
        var quId = 99934L;
        var turnId = 97003L;
        var response =
                dequeueNextTurn(quId)
                        .statusCode(200)
                        .body("id" , equalTo((int)turnId))
                        .body("number", equalTo("ABC125"));

        assertTurnDequeued(turnId);
    }



    private QueueTurnPickRow assertTurnDequeued(long turnId) {
        var pick =
                dao.getFirstRow(
                        "SELECT * FROM QUEUE_TURN_PICK pick " +
                                " WHERE pick.queue_turn_id = :turnId ",
                        QueueTurnPickRow.class,
                        Map.of("turnId", turnId)
                );
        assertNotNull(pick.pickTime);
        assertNull(pick.skipTime);
        assertNull(pick.skipReason);
        assertEquals("ming", pick.serverId);
        assertNotNull(pick.serverDetails);
        return pick;
    }



    private QueueTurnPickRow assertTurnSkipped(long turnId, String reason) {
        var skip =
                dao.getFirstRow(
                        "SELECT * FROM QUEUE_TURN_PICK pick " +
                                " WHERE pick.queue_turn_id = :turnId ",
                        QueueTurnPickRow.class,
                        Map.of("turnId", turnId)
                );
        assertNull(skip.pickTime);
        assertNotNull(skip.skipTime);
        assertEquals(reason, skip.skipReason);
        assertEquals("ming", skip.serverId);
        assertNotNull(skip.serverDetails);
        return skip;
    }


    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_3.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void dequeueQueueWithNoMorePickableTurns(){
        var quId = 99934L;
        var response =
                dequeueNextTurn(quId)
                        .statusCode(406);
    }


    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_3.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void dequeueQueueWithNoTurns(){
        var quId = 99933L;
        var response =
                dequeueNextTurn(quId)
                        .statusCode(406);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_3.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void dequeueStoppedQueue(){
        var quId = 99935L;
        var response =
                dequeueNextTurn(quId)
                        .statusCode(406);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void skipTurn(){
        var quId = 99934L;
        var turnId = 97003L;
        var reason = "keda";
        var response =
                skipNextTurn(quId, reason)
                        .statusCode(200)
                        .body("id" , equalTo((int)turnId))
                        .body("number", equalTo("ABC125"));

        assertTurnSkipped(turnId, reason);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void cancelTurn(){
        var quId = 99934L;
        var turnId = 97003L;
        var response =
                cancelTurn(quId, turnId)
                        .statusCode(204);

        assertTurnCancelled(turnId);
    }



    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_3.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void cancelTurnInStoppedQueue(){
        var quId = 99935L;
        var turnId = 97004L;
        var response =
                cancelTurn(quId, turnId)
                        .statusCode(406);

        assertTurnNotCancelled(turnId);
    }


    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data_3.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void cancelHandledTurn(){
        var quId = 99934L;
        var turnId = 97001L;
        var response =
                cancelTurn(quId, turnId)
                        .statusCode(406);

        assertTurnNotCancelled(turnId);
    }


    @Test
    @Sql(executionPhase = BEFORE_TEST_METHOD, scripts ="sql/queue_test_data.sql")
    @Sql(executionPhase = AFTER_TEST_METHOD, scripts ="sql/clear_database.sql")
    public void cancelTurnByClient(){
        var quId = 99934L;
        var turnId = 97003L;
        var response =
                cancelTurnByCustomer(quId, turnId, "wasta@fake.com")
                        .statusCode(204);

        assertTurnCancelled(turnId);
    }



    private void assertTurnCancelled(long turnId) {
        var turnLeave =
                dao
                .getFirstRow("SELECT * FROM QUEUE_LEAVE " +
                "WHERE queue_turn_id = :turnId"
                , QueueLeaveRow.class
                , Map.of("turnId", turnId));
        assertNotNull(turnLeave.leaveTime);
    }



    private void assertTurnNotCancelled(long turnId) {
        var turnLeave =
                dao
                .getFirstRow("SELECT * FROM QUEUE_LEAVE " +
                                "WHERE queue_turn_id = :turnId"
                        , QueueLeaveRow.class
                        , Map.of("turnId", turnId));
        assertNull(turnLeave);
    }


    private Long countRequests(long id) {
        return dao
                .getSingleResult("SELECT COUNT(*) FROM QUEUE_REQUEST " +
                                " WHERE queue_id = :id",
                        Long.class,
                        Map.of("id", id));
    }


    private String createTurnRequestRequestBody(JsonObject clientDetails) {
        return createTurnRequestRequestBody(null, clientDetails);
    }



    private String createTurnRequestRequestBody(String clientId, JsonObject clientDetails) {
        var jsonBuilder =
                createObjectBuilder()
                .add("client_details", clientDetails);
        if(nonNull(clientId)) jsonBuilder.add("client_id", clientId);
        return jsonBuilder.build().toString();
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


    @Data
    public static class QueueTurnPickRow{
        public Long id;
        public LocalDateTime pickTime;
        public LocalDateTime skipTime;
        public String skipReason;
        public String serverId;
        public String serverDetails;
        public String queueTurnId;
    }


    @Data
    public static class QueueLeaveRow{
        public Long id;
        public Long queueTurnId;
        public LocalDateTime leaveTime;
    }
}




