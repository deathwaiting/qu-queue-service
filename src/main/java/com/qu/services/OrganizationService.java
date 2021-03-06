package com.qu.services;

import com.qu.dto.AdminInvitationCreateRequest;
import com.qu.dto.AdminInvitationCreateResponse;
import com.qu.dto.AdminInvitationDTO;
import com.qu.dto.OrganizationCreateDTO;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public interface OrganizationService {
    Uni<Long> createOrganization(OrganizationCreateDTO organizationDto);
    Uni<AdminInvitationCreateResponse> inviteOrganizationAdmin(AdminInvitationCreateRequest invitation);
    Uni<String> acceptAdminInvitation(String token, String password, String passwordRepeat);
    Multi<AdminInvitationDTO> getAdminInvitations();
    Uni<Void> cancelAdminInvitation(String invitationId);
}
