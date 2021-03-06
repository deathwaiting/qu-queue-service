package com.qu.services;

import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Optional;

public interface SecurityService {
    public static final String ORGANIZATION_CLAIM = "website";
    public static final String SUB_CLAIM = "sub";
    public static final String REALM_ACCESS_CLAIM = "realm_access";
    Optional<Long> getUserOrganizationOptional();
    Long getUserOrganization();
    String getUserId();
    String getUserJwtDecoded();
}
