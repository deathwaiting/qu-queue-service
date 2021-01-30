package com.qu.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.dto.OrganizationCreateDTO;
import com.qu.dto.UserCreationDto;
import com.qu.dto.UserDto;
import com.qu.exceptions.Errors;
import com.qu.exceptions.RuntimeBusinessException;
import com.qu.persistence.entities.Organization;
import io.smallrye.mutiny.Uni;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

import static com.qu.utils.Utils.anyIsNull;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;
import static java.util.Collections.emptyMap;

@ApplicationScoped
public class OrganizationServiceImpl implements OrganizationService{

    private final static Logger LOG = Logger.getLogger(OrganizationServiceImpl.class);

    @Inject
    UserService userService;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Uni<Long> createOrganization(OrganizationCreateDTO organizationDto) {
       return createOwner(organizationDto)
               .flatMap(owner -> doCreateOrganization(owner, organizationDto));
    }



    private Uni<? extends Long> doCreateOrganization(UserDto owner, OrganizationCreateDTO organizationDto) {
        try {
            validateNewOrgData(owner, organizationDto);
            var ownerExtraDetails = objectMapper.writeValueAsString(Optional.ofNullable(owner.extraDetails).orElse(emptyMap()));
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
