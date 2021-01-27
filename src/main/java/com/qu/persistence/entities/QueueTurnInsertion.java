package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "queue_turn_insertion")
public class QueueTurnInsertion extends PanacheEntity {
    @Column(name = "insert_before_turn")
    public Long insertedBeforeTurnId;

    @Column(name = "inserted_turn")
    public Long insertedTurnId;

    @Column(name = "inserted_by")
    public String insertedBy;
}
