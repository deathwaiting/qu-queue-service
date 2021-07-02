package com.qu.services;

import com.qu.commons.enums.UserGroup;
import com.qu.dto.UserCreationDto;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public interface KeycloakService {
    public static final String REALM = "qu";
    public static final String ORG_ROLE_PREFIX = "ORG_";

    String createKeycloakUser(UserCreationDto owner, UserGroup userGroup);

    RoleRepresentation createOrgRole(Long orgId);

    Optional<RoleRepresentation> getOrgRole(Long orgId);

    void addRolesToKeycloakUser(List<RoleRepresentation> userRoles, String userId);
}
