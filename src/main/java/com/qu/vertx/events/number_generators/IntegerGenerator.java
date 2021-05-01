package com.qu.vertx.events.number_generators;

import com.qu.persistence.entities.QueueRequest;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import org.jboss.logging.Logger;
import org.jboss.logmanager.LogManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Iterator;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class IntegerGenerator implements QueueNumberGenerator{
    public static final String NAME = "INTEGER_GEN";
    public static final Logger LOG = Logger.getLogger(IntegerGenerator.class);

    @Inject
    io.vertx.mutiny.pgclient.PgPool client;

    @Override
    @ConsumeEvent(NAME)
    public Uni<String> generate(QueueRequest request) {
        var sql = "with request_order as (" +
                "   select req.id as id, row_number() OVER (ORDER BY req.request_time) AS ord" +
                "   from Queue_request req " +
                "   where req.queue_id = $1 " +
                " ) " +
                " select ord from request_order req" +
                " where req.id = $2";
        return client
                .preparedQuery(sql)
                .execute(Tuple.of(request.getId(), request.getQueue().getId()))
                .map(RowSet::spliterator)
                .map(it -> StreamSupport.stream(it, true))
                .onItem().transformToMulti(Multi.createFrom()::items)
                .collectItems().first().onItem().ifNotNull()
                .transform(row -> row.getString("ord"))
                .onFailure().invoke(LOG::error)
                .onItem().ifNull().continueWith("1");
    }


    @Override
    public String getName() {
        return NAME;
    }
}
