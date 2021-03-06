package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "queue_event_handler")
@Data
public class QueueEventHandler extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "name")
    public String name;

    @Column(name = "event_type")
    public String eventType;

    @Column(name = "common_data")
    public String commonData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_type_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public QueueType queueType;
}
