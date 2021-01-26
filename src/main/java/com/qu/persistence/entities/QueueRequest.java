package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;

import java.time.ZonedDateTime;

import static javax.persistence.FetchType.LAZY;

@Entity
@Table(name= "queue_request")
public class QueueRequest extends PanacheEntity {

    @Id @GeneratedValue
    public Long id;

    @Column(name = "acceptor_id")
    public String acceptorId;

    @Column(name = "client_id")
    public String clientId;

    @Column(name = "request_time")
    @CreationTimestamp
    public ZonedDateTime requestTime;

    @Column(name = "response_time")
    public ZonedDateTime responseTime;

    @Column(name = "client_details")
    public String clientDetails;

    @Column(name = "refused")
    public boolean refused;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "queue_id")
    public Queue queue;
}
