package com.qu.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.commons.enums.UserGroup;
import com.qu.dto.AdminInvitationCreateRequest;
import com.qu.dto.AdminInvitationDTO;
import com.qu.dto.UserCreationDto;
import com.qu.dto.UserDto;
import com.qu.exceptions.Errors;
import com.qu.exceptions.RuntimeBusinessException;
import com.qu.mappers.UserDtoMapper;
import com.qu.persistence.entities.AdminInvitation;
import com.qu.persistence.entities.Organization;
import com.qu.services.mail.params.AdminInviteParameters;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.*;

import static com.qu.commons.constants.Urls.ADMIN_INVITATION_FORM;
import static com.qu.exceptions.Errors.*;
import static com.qu.services.mail.UserMailTemplates.Templates.adminInvite;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;


@ApplicationScoped
public class UserServiceImpl implements UserService{

    private final static Logger LOG = Logger.getLogger(UserServiceImpl.class);
    public static final String ADMIN_INVITATION_EMAIL_SUBJECT = "You have a new invitation to Qu!";

    @Inject
    UserDtoMapper userDtoMapper;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SecurityService securityService;

    @Inject
    ReactiveMailer reactiveMailer;

    @ConfigProperty(name = "com.qu.domain")
    String domain;


    @Override
    public Uni<UserDto> createOrganizationOwner(UserCreationDto owner) {
        //TODO this needs to connect to keycloack and create a user there, for now, it will just return the same DTO
        //while setting the id as a UUID
        UserDto saved = userDtoMapper.toUserDto(owner);
        saved.setId(randomUUID().toString());
        return Uni.createFrom().item(saved);
    }



    @Override
    public List<UserGroup> getUserGroups() {
        return asList(UserGroup.values());
    }



    @Override
    public boolean isValidUserRoles(List<String> roles) {
        return roles
                .stream()
                .allMatch(this::isValidRole);
    }



    @Override
    @Transactional
    public Uni<String> inviteOrganizationAdmin(AdminInvitationCreateRequest invitation) {
        return createAdminInvitation(invitation)
                .flatMap(this::sendInvitationEmail);
    }



    @Override
    @Transactional
    public Uni<String> acceptAdminInvitation(String token, String password) {
        return AdminInvitation
                .<AdminInvitation>findById(token)
                .onItem().ifNull().failWith(new RuntimeBusinessException(NOT_ACCEPTABLE, E$USR$00003))
                .flatMap(invitation -> createAdmin(invitation, password));
    }



    @Override
    public Multi<AdminInvitationDTO> getAdminInvitations() {
        var orgId = securityService.getUserOrganization();
        return AdminInvitation
                .<AdminInvitation>find("SELECT inv FROM AdminInvitation inv WHERE inv.organization.id = ?1", orgId)
                .stream()
                .map(this::toAdminInvitationDto);
    }




    private AdminInvitationDTO toAdminInvitationDto(AdminInvitation invitation) {
        var dto = new AdminInvitationDTO();
        dto.creationTime = invitation.getCreationTime();
        dto.id = invitation.getId();
        dto.email = invitation.getEmail();
        try {
            dto.roles =
                    objectMapper
                    .readValue(ofNullable(invitation.getRoles()).orElse("[]")
                            , new TypeReference<Set<String>>(){});
        } catch (JsonProcessingException e) {
            LOG.error(e,e);
            throw new RuntimeBusinessException(INTERNAL_SERVER_ERROR, E$GEN$00004, dto.roles);
        }
        return dto;
    }



    private Uni<? extends String> createAdmin(AdminInvitation invitation, String password) {
        //TODO this needs to connect to keycloack and create a user there, for now, it will just return the same DTO
        //while setting the id as a UUID
        return Uni.createFrom().item(randomUUID().toString());
    }



    private Uni<String> sendInvitationEmail(AdminInvitationContext ctx) {
        var mailParams = getInvitationMailParams(ctx.token, ctx.organization);
        return adminInvite(mailParams)
                .createUni()
                .map(body -> Mail.withHtml(ctx.adminEmail, ADMIN_INVITATION_EMAIL_SUBJECT, body))
                .flatMap(reactiveMailer::send)
                .chain(() -> Uni.createFrom().item(ctx.token));
    }



    private AdminInviteParameters getInvitationMailParams(String token, Organization org) {
        var mailParams = new AdminInviteParameters();
        mailParams.orgName = org.getName();
        mailParams.acceptUrl = String.format("%s/organization%s?token=%s", domain ,ADMIN_INVITATION_FORM, token);
        return mailParams;
    }



    private Uni<AdminInvitationContext> createAdminInvitation(AdminInvitationCreateRequest invitation) {
        return getOrganizationUni()
                .flatMap(org -> createAdminInvitationInDb(invitation, org))
                .map(invitationEntity -> createAdminInvitationCtx(invitation, invitationEntity));
    }



    private AdminInvitationContext createAdminInvitationCtx(AdminInvitationCreateRequest invitation, AdminInvitation invitationEntity) {
        var ctx = new AdminInvitationContext();
        ctx.adminEmail = invitation.email;
        ctx.organization = invitationEntity.getOrganization();
        ctx.token = invitationEntity.getId();
        return ctx;
    }



    private Uni<AdminInvitation> createAdminInvitationInDb(AdminInvitationCreateRequest invitation, Organization org){
        var id = UUID.randomUUID().toString();
        var invitationEntity = new AdminInvitation();
        invitationEntity.setEmail(invitation.email);
        invitationEntity.setId(id);
        invitationEntity.setRoles(getRoles(invitation));
        invitationEntity.setOrganization(org);
        return invitationEntity.persistAndFlush().chain(() -> Uni.createFrom().item(invitationEntity));
    }



    private String getRoles(AdminInvitationCreateRequest invitation) {
        var roles = ofNullable(invitation.roles).orElse(emptyList());
        validateAdminRoles(roles);
        try{
            return objectMapper.writeValueAsString(roles);
        }catch(Throwable e){
            LOG.error(e,e);
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$GEN$00003, roles.toString());
        }
    }



    private void validateAdminRoles(List<String> roles) {
        if(!isValidUserRoles(roles)){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$USR$00001, roles.toString());
        }
    }



    private Uni<Organization> getOrganizationUni() {
        var orgId = securityService.getUserOrganization();
        return Organization.findById(orgId);
    }



    private boolean isValidRole(String role) {
        return getUserGroups()
                .stream()
                .map(UserGroup::getRoles)
                .flatMap(Set::stream)
                .anyMatch(stdRole -> Objects.equals(stdRole, role));
    }
}




class AdminInvitationContext {
    public String adminEmail;
    public String token;
    public Organization organization;
}