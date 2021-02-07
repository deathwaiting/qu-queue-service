package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "organization_admin_invitation")
@Data
public class AdminInvitation extends PanacheEntityBase {
    @Id
    private String id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "email")
    private String email;

    @Column(name = "roles")
    private String roles;
}