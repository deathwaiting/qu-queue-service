package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.*;
import java.time.ZonedDateTime;

import static javax.persistence.FetchType.LAZY;

@Entity
@Table(name = "queue_leave")
public class QueueLeave extends PanacheEntity {

    @Column(name = "leave_time")
    public ZonedDateTime leaveTime;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "queue_turn_id")
    public QueueTurn turn;
}
