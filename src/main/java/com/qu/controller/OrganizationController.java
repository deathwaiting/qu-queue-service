package com.qu.controller;

import com.qu.commons.constants.Urls;
import com.qu.controller.html.HtmlTemplates;
import com.qu.dto.AdminInvitationCreateRequest;
import com.qu.dto.AdminInvitationCreateResponse;
import com.qu.dto.AdminInvitationDTO;
import com.qu.dto.OrganizationCreateDTO;
import com.qu.exceptions.RuntimeBusinessException;
import com.qu.services.OrganizationService;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import static com.qu.controller.html.HtmlTemplates.Templates.*;

@Path("/organization")
public class OrganizationController {

    @Inject
    OrganizationService orgService;


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Long> createOrganization(OrganizationCreateDTO orgDto){
        return orgService.createOrganization(orgDto);
    }


    @POST
    @Path("/admin/invitation")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<AdminInvitationCreateResponse> inviteAdmin(AdminInvitationCreateRequest invitation){
        return orgService.inviteOrganizationAdmin(invitation);
    }



    @GET
    @Path("/admin/invitation")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Multi<AdminInvitationDTO> getAdminInvitations(){
        return orgService.getAdminInvitations();
    }



    @DELETE
    @Path("/admin/invitation/{invitationId}")
    public Uni<Void> cancelAdminInvitations(@RestPath String invitationId){
        return orgService.cancelAdminInvitation(invitationId);
    }



    @GET
    @Path(Urls.ADMIN_INVITATION_FORM)
    @Produces(MediaType.TEXT_HTML)
    public Uni<String> getAdminRegistrationPage(@RestQuery String token){
        return AdminRegistration(token).createUni();
    }



    @POST
    @Path(Urls.ADMIN_INVITATION_FORM)
    @Produces(MediaType.TEXT_HTML)
    public Uni<String> getAdminRegistrationPage(@RestForm String token, @RestForm String password,@RestForm String passwordRepeat){
        return orgService
                .acceptAdminInvitation(token, password, passwordRepeat)
                .flatMap(id -> AdminRegistrationSuccess(id).createUni())
                .onFailure()
                .recoverWithUni(e -> AdminRegistrationFail(e.getLocalizedMessage()).createUni());
    }
}
