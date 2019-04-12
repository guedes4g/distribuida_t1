package com.pucrs.br.distribuida.t1.dto;

import java.util.HashMap;

/*
    COMMANDS LIST

    1 -> client sends the list of his files
    2 -> client sends a ping to supernode
    3 -> client asks for the list of files in the network
*/
public class Client2Super extends NodeCommunication {

    public Client2Super(int command, Object body) {
        super(command, body);
    }

    public HashMap<String, String> getUploadFilesList() {
        if (this.command == 1)
            return (HashMap<String, String>) this.body;

        return null;
    }
}
