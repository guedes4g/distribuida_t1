package com.pucrs.br.distribuida.t1.dto;

import java.io.Serializable;

public abstract class NodeCommunication implements Serializable {
    protected int command;
    protected Object body;

    public NodeCommunication(int command, Object body) {
        this.body = body;
        this.command = command;
    }

    public int getCommand() {
        return this.command;
    }
}
