package com.qu.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.dto.*;
import com.qu.exceptions.RuntimeBusinessException;
import com.qu.persistence.entities.Organization;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Objects;

import static com.qu.commons.constants.Roles.USER_MANAGER;
import static com.qu.exceptions.Errors.*;
import static com.qu.utils.Utils.anyIsNull;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

@ApplicationScoped
public class OrganizationServiceImpl implements OrganizationService{

    private final static Logger LOG = Logger.getLogger(OrganizationServiceImpl.class);

    @Inject
    UserService userService;


    @Inject
    ObjectMapper objectMapper;

    @Inject
    SecurityService securityService;


    @Inject
    SecurityIdentity securityIdentity;


    @Override
    @PermitAll
    @Transactional
    public Uni<Long> createOrganization(OrganizationCreateDTO organizationDto) {
       return createOwner(organizationDto)
               .flatMap(owner -> doCreateOrganization(owner, organizationDto));
    }



    @Override
    @RolesAllowed(USER_MANAGER)
    @Transactional
    public Uni<AdminInvitationCreateResponse> inviteOrganizationAdmin(AdminInvitationCreateRequest invitation) {
        return userService
                .inviteOrganizationAdmin(invitation)
                .map(AdminInvitationCreateResponse::new);
    }



    @Override
    @PermitAll
    @Transactional
    public Uni<String> acceptAdminInvitation(String token, String password, String passwordRepeat) {
        if(!Objects.equals(password, passwordRepeat)){
            return Uni.createFrom().failure( new RuntimeBusinessException(NOT_ACCEPTABLE, E$USR$00002));
        }
        return userService
                .acceptAdminInvitation(token, password);
    }




    @Override
    @RolesAllowed(USER_MANAGER)
    public Multi<AdminInvitationDTO> getAdminInvitations() {
        return userService.getAdminInvitations();
    }




    @Override
    @RolesAllowed(USER_MANAGER)
    public Uni<Void> cancelAdminInvitation(String invitationId) {
        return userService.cancelAdminInvitation(invitationId);
    }


    private Uni<? extends Long> doCreateOrganization(UserDto owner, OrganizationCreateDTO organizationDto) {
        try {
            validateNewOrgData(owner, organizationDto);
            var ownerExtraDetails = objectMapper.writeValueAsString(ofNullable(owner.extraDetails).orElse(emptyMap()));
            var org = new Organization();
            org.setName(organizationDto.name);
            org.setOwnerId(owner.email);
            org.setOwnerData(ownerExtraDetails);
            return org.persistAndFlush().chain(() -> Uni.createFrom().item(org.getId()));
        } catch (Throwable e) {
            LOG.error(e,e);
            return Uni.createFrom().failure(e);
        }

    }



    private void validateNewOrgData(UserDto owner, OrganizationCreateDTO organizationDto) {
        if(anyIsNull(owner, organizationDto, owner.id, organizationDto.name)){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$GEN$00001);
        }
    }



    private Uni<UserDto> createOwner(OrganizationCreateDTO orgDto) {
        var owner = new UserCreationDto();
        owner.email = orgDto.email;
        owner.name = orgDto.username;
        owner.email = orgDto.email;
        owner.setPassword(orgDto.password);
        return userService.createOrganizationOwner(owner);
    }
}
