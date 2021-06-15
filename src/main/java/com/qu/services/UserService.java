package com.qu.services;

import com.qu.commons.enums.UserGroup;
import com.qu.dto.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;
import java.util.Optional;

public interface UserService {
    public static final String REALM = "qu";
    public static final String ORG_ROLE_PREFIX = "ORG_";

    Uni<UserDto> createOrganizationOwner(UserCreationDto owner);
    List<UserGroup> getUserGroups();
    boolean isValidUserRoles(List<String> roles);
    Uni<String> inviteOrganizationAdmin(AdminInvitationCreateRequest invitation);
    Uni<String> acceptAdminInvitation(String token, String password);
    Multi<AdminInvitationDTO> getAdminInvitations();
    Uni<Void> cancelAdminInvitation(String invitationId);
    void addUserToOrganization(UserDto user);
}
