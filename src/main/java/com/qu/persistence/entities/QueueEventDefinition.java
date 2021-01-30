package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

import javax.persistence.*;

import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "queue_event_definition")
public class QueueEventDefinition extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

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
