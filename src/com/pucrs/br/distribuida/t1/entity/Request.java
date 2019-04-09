package com.pucrs.br.distribuida.t1.entity;

import java.util.HashMap;

public class Request {
    public RequestType command;
    public Object body;

    public Request(RequestType command, Object body) {
        this.body = body;
        this.command = command;
    }

    public boolean isPing() {
        return command == RequestType.Ping;
    }

    public boolean isSendFiles() {
        return command == RequestType.SendFiles;
    }

    public boolean isGetFilesList() {
        return command == RequestType.ReceiveFilesList;
    }

    public HashMap<String, String> getUploadFilesList() {
        if (this.isSendFiles())
            return (HashMap<String, String>) this.body;

        return null;
    }
}
