package com.qu.exceptions;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<RuntimeBusinessException> {
    @Override
    public Response toResponse(RuntimeBusinessException exception) {
        var exceptionBody =
                createObjectBuilder()
                    .add("error_code", exception.getErrorCode().name())
                        .add("message", exception.getMessage())
                        .add("additional_data", createArrayBuilder(exception.getMsgParams()))
                    .build();
        return Response
                .status(exception.getStatus().code())
                .entity(exceptionBody)
                .build();
    }
}
