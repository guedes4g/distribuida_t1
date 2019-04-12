package com.pucrs.br.distribuida.t1.dto;

/*
    COMMANDS LIST

    1 -> new supernode connects into the group and says HI
    2 -> a supernode asks for my list of files
    3 -> a supernode sends me his list of files
*/
public class Super2Super extends NodeCommunication {

    public Super2Super(int command, Object body) {
        super(command, body);
    }

}
