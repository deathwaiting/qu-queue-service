package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

import javax.persistence.*;
import java.time.ZonedDateTime;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "queue_turn_pick")
public class QueueTurnPick extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "pick_time")
    public ZonedDateTime pickTime;

    @Column(name = "skip_name")
    public ZonedDateTime skipTime;

    @Column(name = "skip_reason")
    public String skipReason;

    @Column(name = "server_id")
    public String serverId;

    @Column(name = "server_details")
    public String serverDetails;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "queue_turn_id")
    public QueueTurn turn;
}
