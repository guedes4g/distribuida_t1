package com.pucrs.br.distribuida.t1;

import com.pucrs.br.distribuida.t1.server.ClientNode;
import com.pucrs.br.distribuida.t1.server.SuperNode;

import java.io.IOException;

public class Main {
    public static void main(String args[]) throws IOException {
        if (args.length == 0) {
            System.out.println("For super-node, use: program.java supernode unicastPort multicastPort");
            System.out.println("For client-node, use: program.java clientnode superNodeIpAddress superNodePort");
            System.exit(1);
        }
        
        if(args[0].equals("supernode"))
            new SuperNode(toInt(args[1]), toInt(args[2])).start();
        else
            new ClientNode(args[1], toInt(args[2])).start();
    }

    private static int toInt(String s) {
        return Integer.parseInt(s);
    }
}
