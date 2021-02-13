package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Multi;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.GenerationType.IDENTITY;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "queue_type")
@Data
public class QueueType extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "default_max_size")
    private Integer defaultMaxSize;

    @Column(name = "default_hold_enabled")
    private Boolean defaultHoldEnabled;

    @Column(name = "default_auto_accept_enabled")
    private Boolean defaultAutoAcceptEnabled;

    @Column(name = "organization_id")
    private Long organizationId;

    @OneToMany(mappedBy = "queueType", cascade = ALL)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<QueueEventHandler>  eventHandlers;

    public QueueType(){
        eventHandlers = new HashSet<>();
    }

    public static Multi<QueueType> getByOrganization(Long organizationId){
        return stream("SELECT DISTINCT type FROM QueueType type " +
                        " LEFT JOIN FETCH type.eventHandlers handlers " +
                        " WHERE type.organizationId = :organizationId "
                , Map.of("organizationId", organizationId));
    }
}
