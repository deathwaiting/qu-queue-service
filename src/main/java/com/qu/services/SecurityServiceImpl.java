package com.qu.services;

import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonNumber;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@ApplicationScoped
public class SecurityServiceImpl implements SecurityService{

    @Inject
    JsonWebToken jwt;


    @Override
    public Optional<Long> getUserOrganization() {
        return ofNullable(jwt)
                .map(j -> j.<JsonNumber>getClaim(ORGANIZATION_CLAIM))
                .map(JsonNumber::longValue);
    }
}
