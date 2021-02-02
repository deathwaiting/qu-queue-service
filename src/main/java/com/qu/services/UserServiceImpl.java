package com.qu.services;

import com.qu.commons.enums.UserGroup;
import com.qu.dto.UserCreationDto;
import com.qu.dto.UserDto;
import com.qu.mappers.UserDtoMapper;
import io.smallrye.mutiny.Uni;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;


@ApplicationScoped
public class UserServiceImpl implements UserService{

    @Inject
    UserDtoMapper userDtoMapper;


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



    private boolean isValidRole(String role) {
        return getUserGroups()
                .stream()
                .map(UserGroup::getRoles)
                .flatMap(Set::stream)
                .anyMatch(stdRole -> Objects.equals(stdRole, role));
    }
}
