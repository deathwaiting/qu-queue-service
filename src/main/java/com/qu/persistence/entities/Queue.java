package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.Set;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "queue")
public class Queue extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "name")
    public String name;

    @Column(name = "start_time")
    public ZonedDateTime startTime;

    @Column(name = "end_time")
    public ZonedDateTime endTime;

    @Column(name = "max_size")
    public Integer maxSize;

    @Column(name = "hold_enabled")
    public boolean holdEnabled;

    @Column(name = "organization_id")
    public Long organizationId;

    @Column(name = "auto_accept_enabled")
    public Boolean autoAcceptEnabled;


    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "queue_type_id")
    public QueueType type;

    @OneToMany(mappedBy = "queue", fetch = LAZY, cascade = ALL)
    public Set<QueueRequest> requests;


    @OneToMany(mappedBy = "queue", fetch = LAZY)
    public Set<QueueAction> actions;
}
