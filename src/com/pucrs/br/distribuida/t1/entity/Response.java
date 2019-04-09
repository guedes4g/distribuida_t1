package com.pucrs.br.distribuida.t1.entity;

import java.util.List;

public class Response {
    private ResponseType command;
    private Object body;

    public Response(ResponseType command, Object body) {
        this.body = body;
        this.command = command;
    }

    public boolean isFilesList() {
        return command == ResponseType.FilesList;
    }

    public List<FileData> getFilesList() {
        if (isFilesList())
            return (List<FileData>) this.body;

        return null;
    }
}
