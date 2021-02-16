package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.AUTO;
import static javax.persistence.GenerationType.IDENTITY;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name= "queue_request")
@Data
public class QueueRequest extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "acceptor_id")
    public String acceptorId;

    @Column(name = "client_id")
    public String clientId;

    @Column(name = "request_time")
    @CreationTimestamp
    public LocalDateTime requestTime;

    @Column(name = "response_time")
    public LocalDateTime responseTime;

    @Column(name = "client_details")
    public String clientDetails;

    @Column(name = "refused")
    public Boolean refused;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "queue_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public Queue queue;


    @OneToOne(fetch = LAZY, mappedBy = "request")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public QueueTurn turn;

}
