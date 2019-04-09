package com.pucrs;

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
