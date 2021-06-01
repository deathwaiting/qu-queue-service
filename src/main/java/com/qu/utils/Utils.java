package com.qu.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class Utils {
    static  public boolean anyIsNull(Object... objects){
        return ofNullable(objects)
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList)
                .stream()
                .anyMatch(Objects::isNull);
    }



    static  public boolean allIsNull(Object... objects){
        return ofNullable(objects)
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList)
                .stream()
                .allMatch(Objects::isNull);
    }
}
