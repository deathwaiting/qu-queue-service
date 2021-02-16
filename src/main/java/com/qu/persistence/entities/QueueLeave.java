package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "queue_leave")
@Data
@EqualsAndHashCode(callSuper = true)
public class QueueLeave extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "leave_time")
    public LocalDateTime leaveTime;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "queue_turn_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public QueueTurn turn;
}
