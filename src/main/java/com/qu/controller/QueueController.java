package com.qu.controller;

import com.qu.dto.QueueDto;
import com.qu.dto.QueueTypeDto;
import com.qu.services.queue.event.QueueManagementService;
import com.qu.services.queue.event.model.QueueEventHandlerInfo;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import javax.inject.Inject;
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
}
