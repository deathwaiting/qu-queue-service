package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.*;

@Entity
@Table(name = "queue_event_definition")
public class QueueEventDefinition extends PanacheEntity {

    @Column(name = "name")
    public String name;

    @Column(name = "event_type")
    public String eventType;

    @Column(name = "event_data")
    public String eventData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_type_id")
    public QueueType queueType;
}
