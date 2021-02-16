package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "queue_turn")
@Data
@EqualsAndHashCode(callSuper = true)
public class QueueTurn extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;


    @Column(name = "queue_number")
    public String queueNumber;

    @Column(name = "enqueue_time")
    public LocalDateTime enqueueTime;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "request_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public QueueRequest request;


    @OneToOne(fetch = LAZY, mappedBy = "turn")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public QueueLeave leave;


    @OneToOne(fetch = LAZY, mappedBy = "turn")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public QueueTurnMove turnMove;



    @OneToOne(fetch = LAZY, mappedBy = "turn")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public QueueTurnPick pick;
}
