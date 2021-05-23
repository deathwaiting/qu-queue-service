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
}
