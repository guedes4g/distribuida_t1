package com.pucrs.br.distribuida.t1.dto;

import com.pucrs.br.distribuida.t1.entity.FileData;

import java.util.ArrayList;
import java.util.List;

/*
    COMMANDS LIST

    1 -> supernode sends the list of files in network to client
*/
public class Super2Client extends NodeCommunication {

    public Super2Client(int command, Object body) {
        super(command, body);
    }

    public ArrayList<FileData> getFilesList() {
        if (command == 1)
            return (ArrayList<FileData>) this.body;

        return null;
    }
}
