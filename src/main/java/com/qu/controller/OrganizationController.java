package com.qu.controller;

import com.qu.dto.OrganizationCreateDTO;
import com.qu.services.OrganizationService;
import io.smallrye.mutiny.Uni;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/organization")
public class OrganizationController {

    @Inject
    OrganizationService orgService;


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Long> createOrganization(OrganizationCreateDTO orgDto){
        return orgService.createOrganization(orgDto);
    }

}
