package com.pucrs.br.distribuida.t1.dto;

import com.pucrs.br.distribuida.t1.entity.FileData;

import java.util.HashMap;

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

    public HashMap<String,FileData> getFilesMap() {
        return (HashMap<String,FileData>) this.body;
    }
}
