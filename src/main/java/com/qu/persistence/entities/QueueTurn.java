package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.*;
import java.time.ZonedDateTime;

import static javax.persistence.FetchType.LAZY;

@Entity
@Table(name = "queue_turn")
public class QueueTurn extends PanacheEntity {
    @Column(name = "queue_number")
    public String queueNumber;

    @Column(name = "enqueue_time")
    public ZonedDateTime enqueueTime;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "request_id")
    public QueueRequest request;


    @OneToOne(fetch = LAZY, mappedBy = "turn")
    public QueueLeave leave;
}
