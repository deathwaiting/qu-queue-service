package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "queue_turn_insertion")
@Data
@EqualsAndHashCode(callSuper = true)
public class QueueTurnMove extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "insert_before_turn")
    public Long insertedBeforeTurnId;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "inserted_turn")
    public QueueTurn turn;

    @Column(name = "inserted_by")
    public String insertedBy;
}
