package com.qu.persistence.entities;

import com.qu.dto.QueueListParams;
import com.qu.dto.QueueListResponse;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static javax.persistence.CascadeType.ALL;
import static javax.persistence.FetchType.LAZY;
import static javax.persistence.GenerationType.IDENTITY;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "queue")
@Data
public class Queue extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "name")
    public String name;

    @Column(name = "start_time")
    public LocalDateTime startTime;

    @Column(name = "end_time")
    public LocalDateTime endTime;

    @Column(name = "max_size")
    public Integer maxSize;

    @Column(name = "hold_enabled")
    public Boolean holdEnabled;

    @Column(name = "auto_accept_enabled")
    public Boolean autoAcceptEnabled;


    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "queue_type_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public QueueType type;



    @OneToMany(mappedBy = "queue", fetch = LAZY, cascade = ALL)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public Set<QueueRequest> requests;



    @OneToMany(mappedBy = "queue", fetch = LAZY)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    public Set<QueueAction> actions;

    @Column(name = "number_generator")
    public String numberGenerator;



    public static Uni<QueueListPage> getQueuesByOrganization(Long orgId, QueueListParams params) {
        var query=
                Queue
                .find("SELECT qu FROM Queue qu " +
                                " LEFT JOIN qu.type type " +
                                " LEFT JOIN FETCH qu.actions actions " +
                                " WHERE type.organizationId = :orgId " +
                                " ORDER BY qu.startTime desc "
                    , Map.of("orgId", orgId))
                .page(params.pageNum, params.pageSize);
        var totalPgCount =
                Queue
                .find("SELECT qu FROM Queue qu " +
                        " LEFT JOIN qu.type type " +
                        "  WHERE type.organizationId = :orgId "
                        , Map.of("orgId", orgId))
                .page(params.pageNum, params.pageSize);
        var data = query.stream().map(Queue.class::cast).collectItems().asList();
        return data
                .chain(rows -> totalPgCount.pageCount().map(cnt -> new QueueListPage(cnt, rows)));
    }


    public static Uni<Queue> getQueueFullDetailsById(Long id, Long orgId) {
        return Queue
                .find( "SELECT DISTINCT qu FROM Queue qu " +
                                " LEFT JOIN FETCH qu.type type " +
                                " LEFT JOIN FETCH qu.actions actions " +
                                " LEFT JOIN FETCH qu.requests requests " +
                                " LEFT JOIN FETCH requests.turn turn " +
                                " LEFT JOIN FETCH turn.leave leave " +
                                " LEFT JOIN FETCH turn.turnMove move " +
                                " LEFT JOIN FETCH turn.pick pick " +
                                " WHERE type.organizationId = :orgId " +
                                " AND qu.id = :id " +
                                " ORDER BY qu.startTime desc "
                        , Map.of("id", id , "orgId", orgId))
                .firstResult();
    }



    public static Uni<Queue> getQueueFullDetailsById(Long id) {
        return Queue
                .find( "SELECT DISTINCT qu FROM Queue qu " +
                                " LEFT JOIN FETCH qu.type type " +
                                " LEFT JOIN FETCH qu.actions actions " +
                                " LEFT JOIN FETCH qu.requests requests " +
                                " LEFT JOIN FETCH requests.turn turn " +
                                " LEFT JOIN FETCH turn.leave leave " +
                                " LEFT JOIN FETCH turn.turnMove move " +
                                " LEFT JOIN FETCH turn.pick pick " +
                                " WHERE qu.id = :id " +
                                " ORDER BY qu.startTime desc "
                        , Map.of("id", id ))
                .firstResult();
    }



    public static Uni<Queue> findByIdAndOrganizationId(Long id, Long orgId) {
        return Queue
                .find( "SELECT qu FROM Queue qu " +
                                " LEFT JOIN FETCH qu.type type " +
                                " LEFT JOIN FETCH qu.actions actions " +
                                " WHERE type.organizationId = :orgId " +
                                " AND qu.id = :id " +
                                " ORDER BY qu.startTime desc "
                        , Map.of("id", id , "orgId", orgId))
                .firstResult();
    }


    public static class QueueListPage{
        public Integer totalPagesCount;
        public List<Queue> page;

        public QueueListPage(Integer pgCount, List<Queue> page){
            this.totalPagesCount = pgCount;
            this.page = page;
        }
    }
}
