package com.qu.controller;

import com.qu.commons.enums.QueueActionType;
import com.qu.dto.*;
import com.qu.services.QueueManagementService;
import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/queue")
public class QueueController {

    @Inject
    QueueManagementService queueMgrService;

    @POST
    @Path("/type")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Long> createQueueType(QueueTypeDto queueType){
        return queueMgrService.createQueueType(queueType);
    }


    @GET
    @Path("/event/handlers")
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<QueueEventHandlerInfo<?>> getAllEventHandlers(){
        return queueMgrService.getAllEventHandlers();
    }



    @GET
    @Path("/type")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Multi<QueueTypeDto> getQueueTypes(){
        return queueMgrService.getQueueTypes();
    }



    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Long> createQueue(QueueDto queue){
        return queueMgrService.createQueue(queue);
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<QueueListResponse> getQueueList(
            @DefaultValue("30") @RestQuery("page_count") Integer pageCount
            , @DefaultValue("0") @RestQuery("page_num") Integer pageIndex){
        var params = new QueueListParams();
        params.pageNum = pageIndex;
        params.pageSize = pageCount;
        return queueMgrService.getQueueList(params);
    }



    @GET()
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<QueueDetailsDto> getQueue(@RestPath Long id){
        return queueMgrService.getQueue(id);
    }


    @POST
    @Path("/{id}/action")
    public Uni<Void> setQueueStatus(@NotNull @RestPath("id")Long id, @NotNull @RestQuery("action") QueueActionType action){
        return queueMgrService.setQueueStatus(id, action);
    }



    @POST
    @Path("/{id}/turn")
    public Uni<QueueTurnDto> addTurn(@NotNull @RestPath("id")Long id, @NotNull QueueTurnCreateDto turn){
        turn.queueId = id;
        return queueMgrService.enqueue(turn);
    }


    @POST
    @Path("/{id}/turn/request")
    public Uni<QueueRequestAnswerDto> makeTurnRequest(@NotNull @RestPath("id")Long id, @NotNull QueueRequestCreateDto request){
        request.queueId = id;
        return queueMgrService.makeRequest(request);
    }



    @POST
    @Path("/{id}/turn/request/{request}/accept")
    public Uni<QueueTurnDto> acceptTurnRequest(@NotNull @RestPath("id")Long id, @NotNull @RestPath("request")Long requestId){
        return queueMgrService.acceptRequest(id, requestId);
    }


    @POST
    @Path("/{id}/turn/request/{request}/deny")
    public Uni<Void> denyTurnRequest(@NotNull @RestPath("id")Long id, @NotNull @RestPath("request")Long requestId){
        return queueMgrService.denyRequest(id, requestId);
    }


    @GET
    @Path("/{id}/dequeue")
    public Uni<QueueTurnDto> dequeue(@NotNull @RestPath("id")Long id){
        return queueMgrService.dequeue(id);
    }


    @POST
    @Path("/{id}/skip")
    public Uni<QueueTurnDto> skipTurn(@NotNull @RestPath("id")Long id, @RestQuery("reason")String skipReason){
        return queueMgrService.skipTurn(id, skipReason);
    }


    @DELETE
    @Path("/{queueId}/turn/{turnId}")
    public Uni<Void> cancelTurn(@NotNull @RestPath("queueId")Long queueId, @NotNull @RestPath("turnId")Long turnId){
        return queueMgrService.cancelTurn(queueId, turnId);
    }



    @DELETE
    @Path("/{queueId}/turn/{turnId}/by_customer")
    public Uni<Void> cancelTurnByCustomer(@NotNull @RestPath("queueId")Long queueId, @NotNull @RestPath("turnId")Long turnId, @NotNull @RestQuery("client_id")String clientId){
        return queueMgrService.cancelTurnByCustomer(queueId, turnId, clientId);
    }

}
