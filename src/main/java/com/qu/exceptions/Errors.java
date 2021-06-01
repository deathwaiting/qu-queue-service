package com.qu.exceptions;

public enum Errors {
    E$GEN$00001("Missing required data!"),
    E$GEN$00002("No Organization found for the user!"),
    E$GEN$00003("Failed to write object as json string! object[%s]"),
    E$GEN$00004("Failed to parse json string! string[%s]"),
    E$GEN$00005("Failed to fetch current user!"),


    E$USR$00001("Invalid roles [%s]!"),
    E$USR$00002("Passwords didn't match!"),
    E$USR$00003("Invalid invitation token!"),
    E$USR$00004("Failed to parse JWT token!"),

    E$QUE$00001("Failed to create Queue type! cause: %s"),
    E$QUE$00002("Queue type doesn't exists!"),
    E$QUE$00003("Queue with Id[%d] doesn't exists!"),
    E$QUE$00004("Cannot change queue state from [%s] to [%s]!"),
    E$QUE$00005("Failed to create queue turn!"),
    E$QUE$00006("Cannot request Turns for that Queue! Queue is not accepting requests!"),
    E$QUE$00007("Cannot request Turns for that Queue! Queue is Full!"),
    E$QUE$00008("Cannot find turn request!"),
    E$QUE$00009("Invalid turn request! Already handled!"),
    E$QUE$00010("Failed to pick the queue turn!"),
    E$QUE$00011("Queue has no more turns!"),
    E$QUE$00012("Invalid Queue status!")

    ;

    private String msg;

    Errors(String msg){
        this.msg = msg;
    }

    String getMessage(){
        return msg;
    }
}
