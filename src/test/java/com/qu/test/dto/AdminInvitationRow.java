package com.qu.test.dto;

import lombok.Data;

@Data
public class AdminInvitationRow{
    private String id;
    private Long organizationId;
    private String email;
    private String roles;
}