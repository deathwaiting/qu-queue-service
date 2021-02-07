package com.qu.services;

import com.qu.exceptions.RuntimeBusinessException;
import org.eclipse.microprofile.jwt.JsonWebToken;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonNumber;
import java.util.Optional;

import static com.qu.exceptions.Errors.E$GEN$00002;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_ACCEPTABLE;
import static java.util.Optional.ofNullable;

@ApplicationScoped
public class SecurityServiceImpl implements SecurityService{

    @Inject
    JsonWebToken jwt;


    @Override
    public Optional<Long> getUserOrganizationOptional() {
        return ofNullable(jwt)
                .map(j -> j.<JsonNumber>getClaim(ORGANIZATION_CLAIM))
                .map(JsonNumber::longValue);
    }

    @Override
    public Long getUserOrganization() {
        return getUserOrganizationOptional()
                .orElseThrow(() -> new RuntimeBusinessException(NOT_ACCEPTABLE, E$GEN$00002));
    }


}
