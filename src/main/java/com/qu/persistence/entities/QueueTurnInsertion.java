package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;

import javax.persistence.*;

import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "queue_turn_insertion")
public class QueueTurnInsertion extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "insert_before_turn")
    public Long insertedBeforeTurnId;

    @Column(name = "inserted_turn")
    public Long insertedTurnId;

    @Column(name = "inserted_by")
    public String insertedBy;
}
