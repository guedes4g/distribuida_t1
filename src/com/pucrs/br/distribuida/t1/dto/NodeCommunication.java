package com.pucrs.br.distribuida.t1.dto;

public abstract class NodeCommunication {
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
