package com.qu.services;

import com.qu.dto.OrganizationCreateDTO;
import io.smallrye.mutiny.Uni;

public interface OrganizationService {
    Uni<Long> createOrganization(OrganizationCreateDTO organizationDto);
}
