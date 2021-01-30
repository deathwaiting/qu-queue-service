package com.qu.exceptions;

public enum Errors {
    E$GEN$00001("Missing required data!");

    private String msg;

    Errors(String msg){
        this.msg = msg;
    }

    String getMessage(){
        return msg;
    }
}
