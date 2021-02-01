package com.qu.persistence.entities;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@Entity
@Table(name = "organization_admin_invitation")
public class AdminInvitation {
    @Id
    public String id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "organization_id")
    public Organization organization;

    @Column(name = "email")
    public String email;

    @Column(name = "roles")
    public String roles;
}
