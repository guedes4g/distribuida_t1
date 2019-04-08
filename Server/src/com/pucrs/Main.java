package com.pucrs;


import java.io.IOException;

public class Main {
    public static void main(String args[]) throws IOException {
        if(args[0].equals("supernode"))
            new NameNodeServer().start();
        else
            new ClientNodeServer(args[1],
                    Integer.parseInt(args[2])).start();
    }
}
