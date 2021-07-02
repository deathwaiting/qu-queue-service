package com.qu.services;

import com.qu.commons.enums.UserGroup;
import com.qu.dto.UserCreationDto;
import com.qu.exceptions.RuntimeBusinessException;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static com.qu.exceptions.Errors.E$USR$00005;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@ApplicationScoped
public class KeycloakServiceImpl implements KeycloakService{

    private final static Logger LOG = Logger.getLogger(KeycloakServiceImpl.class);
    @Inject
    Keycloak keycloak;


    @Override
    public String createKeycloakUser(UserCreationDto owner, UserGroup userGroup) {
        var userRoles = createKeycloakUserRoles(owner, userGroup);
        var credentialRepresentation = createUserCredentials(owner);
        var userId = doCreateKeyCloakUser(owner, credentialRepresentation);
        addRolesToKeycloakUser(userRoles, userId);
        return userId;
    }



    @Override
    public RoleRepresentation createOrgRole(Long orgId) {
        var orgRoleName = getOrgRoleName(orgId);
        var orgRole = new RoleRepresentation();
        orgRole.setName(orgRoleName);
        keycloak.realm(REALM).roles().create(orgRole);
        return keycloak.realm(REALM).roles().get(orgRoleName).toRepresentation();
    }


    private String getOrgRoleName(Long orgId) {
        return format("%s%d", ORG_ROLE_PREFIX, orgId);
    }


    @Override
    public Optional<RoleRepresentation> getOrgRole(Long orgId){
        try{
            var orgRoleName = getOrgRoleName(orgId);
            return ofNullable(keycloak.realm(REALM).roles().get(orgRoleName))
                    .map(RoleResource::toRepresentation);
        }catch(Throwable e){
            LOG.error(e,e);
            return Optional.empty();
        }
    }


    private String doCreateKeyCloakUser(UserCreationDto userDto, CredentialRepresentation credentialRepresentation) {
        var userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userDto.email);
        userRepresentation.setFirstName(userDto.name);
        userRepresentation.setEmail(userDto.email);
        userRepresentation.setEnabled(true);
        userRepresentation.setCredentials(asList(credentialRepresentation));

        var usersResource = keycloak.realm(REALM).users();
        var response = usersResource.create(userRepresentation);

        if(response.getStatus() >= 400){
            LOG.errorf("Failed to process user at keycloak!" +
                    " got response with status [%d/%s]", response.getStatus() ,response.getStatusInfo().toEnum().toString());
            throw new RuntimeBusinessException(INTERNAL_SERVER_ERROR, E$USR$00005);
        }
        return getKeycloakUserId(response);
    }



    private String getKeycloakUserId(javax.ws.rs.core.Response response) {
        return response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
    }


    @Override
    public void addRolesToKeycloakUser(List<RoleRepresentation> userRoles, String userId) {
        keycloak.realm(REALM).users().get(userId).roles().realmLevel().add(userRoles);
    }


    private List<RoleRepresentation> createKeycloakUserRoles(UserCreationDto user, UserGroup userGroup) {
        var allRoles = keycloak.realm(REALM).roles().list("ROLE_", false);
        return allRoles.stream().filter(role -> userGroup.getRoles().contains(role.getName())).collect(toList());
    }


    private CredentialRepresentation createUserCredentials(UserCreationDto owner) {
        var credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(owner.getPassword());
        return credentialRepresentation;
    }
}
