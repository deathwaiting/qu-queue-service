package com.qu.dto;

import java.time.ZonedDateTime;
import java.util.Set;

public class AdminInvitationDTO {
    public String id;
    public String email;
    public ZonedDateTime creationTime;
    public Set<String> roles;
}
