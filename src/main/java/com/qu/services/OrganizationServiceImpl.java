package com.qu.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.commons.enums.UserGroup;
import com.qu.dto.*;
import com.qu.exceptions.Errors;
import com.qu.exceptions.RuntimeBusinessException;
import com.qu.persistence.entities.AdminInvitation;
import com.qu.persistence.entities.Organization;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.qu.commons.constants.Roles.USER_MANAGER;
import static com.qu.exceptions.Errors.*;
import static com.qu.utils.Utils.anyIsNull;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;
import static java.util.Collections.emptyList;
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
    public Uni<Long> createOrganization(OrganizationCreateDTO organizationDto) {
       return createOwner(organizationDto)
               .flatMap(owner -> doCreateOrganization(owner, organizationDto));
    }



    @Override
    @RolesAllowed(USER_MANAGER)
    @Transactional
    public Uni<AdminInvitationCreateResponse> inviteOrganizationAdmin(AdminInvitationCreateRequest invitation) {
        return createAdminInvitation(invitation)
                .flatMap(this::sendInvitationEmail)
                .map(AdminInvitationCreateResponse::new);
    }



    private Uni<String> sendInvitationEmail(String token) {
        return Uni.createFrom().item(token);
    }



    private Uni<String> createAdminInvitation(AdminInvitationCreateRequest invitation) {
        var id = UUID.randomUUID().toString();
        var invitationEntity = new AdminInvitation();
        invitationEntity.setEmail(invitation.email);
        invitationEntity.setId(id);
        invitationEntity.setRoles(getRoles(invitation));
        return getOrganizationUni()
                .flatMap(org -> {invitationEntity.setOrganization(org); return invitationEntity.persistAndFlush();})
                    .chain(() -> Uni.createFrom().item(id));
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
        if(!userService.isValidUserRoles(roles)){
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, E$USR$00004, roles.toString());
        }
    }



    private Uni<Organization> getOrganizationUni() {
        var orgId =   securityService
                        .getUserOrganization()
                        .orElseThrow(() -> new RuntimeBusinessException(NOT_ACCEPTABLE, E$GEN$00002));
        return Organization.findById(orgId);
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
            throw new RuntimeBusinessException(NOT_ACCEPTABLE, Errors.E$GEN$00001);
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
