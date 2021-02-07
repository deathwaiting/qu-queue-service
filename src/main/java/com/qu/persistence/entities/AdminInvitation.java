package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

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

    @Column(name = "creation_time")
    @CreationTimestamp
    private LocalDateTime creationTime;
}
