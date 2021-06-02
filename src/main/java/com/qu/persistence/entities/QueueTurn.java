package com.qu.persistence.entities;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.IDENTITY;


@Entity
@Table(name = "queue_turn")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QueueTurn extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "queue_number")
    public String queueNumber;

    @Column(name = "enqueue_time")
    @CreationTimestamp
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

    public QueueTurn(QueueRequest request, String queueNum){
        this.request = request;
        this.queueNumber = queueNum;
    }


    public static Uni<QueueTurn> findFullDataByIdAndOrgIdAndQuId(Long turnId, Long orgId, Long quId){
        return QueueTurn.<QueueTurn>find(
                "SELECT turn from QueueTurn turn " +
                        " LEFT JOIN FETCH turn.request req " +
                        " LEFT JOIN FETCH req.queue qu " +
                        " LEFT JOIN FETCH qu.type type " +
                        " LEFT JOIN FETCH qu.actions actions " +
                        " LEFT JOIN FETCH turn.leave leave " +
                        " LEFT JOIN FETCH turn.turnMove move " +
                        " LEFT JOIN FETCH turn.pick pick " +
                        " WHERE turn.id = :turnId " +
                        " AND type.organizationId = :orgId " +
                        " AND qu.id = :quId"
                    , Map.of("turnId", turnId
                        ,"orgId", orgId
                        , "quId", quId) )
                .firstResult();
    }



    public static Uni<QueueTurn> findFullDataByIdAndClientIdAndQuId(Long turnId, String clientId, Long quId) {
        return QueueTurn.<QueueTurn>find(
                "SELECT turn from QueueTurn turn " +
                        " LEFT JOIN FETCH turn.request req " +
                        " LEFT JOIN FETCH req.queue qu " +
                        " LEFT JOIN FETCH qu.type type " +
                        " LEFT JOIN FETCH qu.actions actions " +
                        " LEFT JOIN FETCH turn.leave leave " +
                        " LEFT JOIN FETCH turn.turnMove move " +
                        " LEFT JOIN FETCH turn.pick pick " +
                        " WHERE turn.id = :turnId " +
                        " AND req.clientId = :clientId " +
                        " AND qu.id = :quId"
                , Map.of("turnId", turnId
                        , "clientId", clientId
                        , "quId", quId) )
                .firstResult();
    }
}
