package com.qu.exceptions;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
public class RuntimeBusinessException extends RuntimeException {

    private Errors errorCode;
    private HttpResponseStatus status;
    private List<?> msgParams;

    public RuntimeBusinessException(HttpResponseStatus status, Errors errorCode, Object ... params){
        super(createMessage(errorCode, params));
        this.status = status;
        this.errorCode = errorCode;
        this.msgParams = Optional.ofNullable(params).map(Arrays::asList).orElseGet(Collections::emptyList);
    }


    private static String createMessage(Errors errorCode, Object[] params) {
        return String.format(errorCode.getMessage(), params);
    }
}
