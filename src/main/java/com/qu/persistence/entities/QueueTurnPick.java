package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.ZonedDateTime;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "queue_turn_pick")
@Data
public class QueueTurnPick extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "pick_time")
    public ZonedDateTime pickTime;

    @Column(name = "skip_time")
    public ZonedDateTime skipTime;

    @Column(name = "skip_reason")
    public String skipReason;

    @Column(name = "server_id")
    public String serverId;

    @Column(name = "server_details")
    public String serverDetails;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "queue_turn_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public QueueTurn turn;
}
