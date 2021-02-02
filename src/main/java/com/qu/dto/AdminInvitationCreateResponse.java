package com.qu.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@NoArgsConstructor
public class AdminInvitationCreateResponse {
    public String invitationToken;

    public AdminInvitationCreateResponse(String token){
        invitationToken = token;
    }
}
