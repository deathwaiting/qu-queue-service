package com.qu.exceptions;

public enum Errors {
    E$GEN$00001("Missing required data!"),
    E$GEN$00002("No Organization found for the user!"),
    E$GEN$00003("Failed to write object as json string! object[%s]"),

    E$USR$00004("Invalid roles [%s]!");

    private String msg;

    Errors(String msg){
        this.msg = msg;
    }

    String getMessage(){
        return msg;
    }
}
