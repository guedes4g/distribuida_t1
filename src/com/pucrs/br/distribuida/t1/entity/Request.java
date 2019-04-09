package com.pucrs.br.distribuida.t1.entity;

import java.util.HashMap;

public class Request {
    public int command;
    public Object body;
    
    public <B> B getBody() {
        return (B) body;
    }
    
    public void teste() {
        this.getBody();
    }
}
