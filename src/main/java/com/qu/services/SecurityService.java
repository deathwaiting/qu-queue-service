package com.qu.services;

import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Optional;

public interface SecurityService {
    public static final String ORGANIZATION_CLAIM = "organizationId";
    Optional<Long> getUserOrganization();
}
