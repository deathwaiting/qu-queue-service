package com.qu.services;

import com.qu.commons.enums.UserGroup;
import com.qu.dto.AdminInvitationCreateRequest;
import com.qu.dto.AdminInvitationCreateResponse;
import com.qu.dto.UserCreationDto;
import com.qu.dto.UserDto;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface UserService {
    Uni<UserDto> createOrganizationOwner(UserCreationDto owner);
    List<UserGroup> getUserGroups();
    boolean isValidUserRoles(List<String> roles);
    Uni<String> inviteOrganizationAdmin(AdminInvitationCreateRequest invitation);
    Uni<String> acceptAdminInvitation(String token, String password);
}
