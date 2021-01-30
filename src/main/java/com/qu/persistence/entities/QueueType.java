package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

import javax.persistence.*;
import java.util.Set;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "queue_type")
public class QueueType extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "name")
    public String name;

    @Column(name = "default_max_size")
    public Integer defaultMaxSize;

    @Column(name = "default_hold_enabled")
    public Boolean defaultHoldEnabled;

    @Column(name = "default_auto_accept_enabled")
    public Boolean defaultAutoAcceptEnabled;

    @Column(name = "organization_id")
    public Long organizationId;

    @OneToMany(mappedBy = "queueType", cascade = ALL)
    public Set<QueueEventDefinition>  events;
}
