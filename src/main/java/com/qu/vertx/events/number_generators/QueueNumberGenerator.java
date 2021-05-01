package com.qu.vertx.events.number_generators;

import com.qu.persistence.entities.QueueRequest;
import io.smallrye.mutiny.Uni;

public interface QueueNumberGenerator {
    Uni<String> generate(QueueRequest request);
    String getName();
}
