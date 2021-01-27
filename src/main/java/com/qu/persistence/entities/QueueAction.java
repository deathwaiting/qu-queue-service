package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;

import java.time.ZonedDateTime;

import static javax.persistence.FetchType.LAZY;

@Entity
@Table(name = "queue_action")
public class QueueAction extends PanacheEntity {
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "queue_id")
    public Queue queue;

    @CreationTimestamp
    public ZonedDateTime actionTime;

    @Column(name = "action_type")
    public String actionType;
}
