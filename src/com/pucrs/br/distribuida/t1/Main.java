package com.pucrs.br.distribuida.t1;

import com.pucrs.br.distribuida.t1.server.ClientNode;
import com.pucrs.br.distribuida.t1.server.SuperNode;

import java.io.IOException;

public class Main {
    public static void main(String args[]) throws IOException {
        if (args.length == 0) {
            System.out.println("Use program.java supernode|server-p2p [hostname port]");
            System.exit(1);
        }
        
        if(args[0].equals("supernode"))
            new SuperNode().start();
        else
            new ClientNode(args[1], Integer.parseInt(args[2])).start();
    }
}
