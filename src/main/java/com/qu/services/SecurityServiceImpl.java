package com.qu.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qu.exceptions.RuntimeBusinessException;
import com.qu.utils.Utils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jboss.logmanager.LogManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.qu.exceptions.Errors.*;
import static com.qu.services.KeycloakService.ORG_ROLE_PREFIX;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

@ApplicationScoped
public class SecurityServiceImpl implements SecurityService{

    final private static Logger logger = Logger.getLogger(SecurityServiceImpl.class);

    @Inject
    JsonWebToken jwt;

    @Inject
    ObjectMapper objectMapper;


    @Override
    public Optional<Long> getUserOrganizationOptional() {
        return ofNullable(jwt)
                .map(j -> j.<JsonObject>getClaim(REALM_ACCESS_CLAIM))
                .map(claim -> claim.asJsonObject().getJsonArray("roles"))
                .flatMap(this::getOrganizationRole)
                .map(roleName -> roleName.replace(ORG_ROLE_PREFIX, ""))
                .flatMap(Utils::parseLongSafely);
    }



    private Optional<String> getOrganizationRole(javax.json.JsonArray roles) {
        return roles.getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .filter(roleName -> roleName.startsWith(ORG_ROLE_PREFIX))
                .findFirst();
    }


    @Override
    public Long getUserOrganization() {
        return getUserOrganizationOptional()
                .orElseThrow(() -> new RuntimeBusinessException(NOT_ACCEPTABLE, E$GEN$00002));
    }



    @Override
    public String getUserId() {
        return ofNullable(jwt)
                .map(j -> j.<String>getClaim(SUB_CLAIM))
                .orElseThrow(() -> new RuntimeBusinessException(INTERNAL_SERVER_ERROR, E$GEN$00005));
    }



    @Override
    public String getUserJwtDecoded() {
        return ofNullable(jwt)
                .map(this::toJsonString)
                .orElseThrow(() -> new RuntimeBusinessException(INTERNAL_SERVER_ERROR, E$GEN$00005));
    }



    private String toJsonString(JsonWebToken jwt){
        return jwt
                .getClaimNames()
                .stream()
                .collect(
                        collectingAndThen(
                                toMap(Function.identity(), jwt::getClaim)
                                , this::toJsonString
                        ));
    }


    private String toJsonString(Map<String, ?> map){
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.error(e,e);
            throw new RuntimeBusinessException(INTERNAL_SERVER_ERROR, E$USR$00004);
        }
    }
}
