package com.pucrs;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class NameNodeServer {
    ServerSocket server;
    ArrayList<Node> nodes;

    public NameNodeServer() throws IOException {
        this.server = new ServerSocket(3333);
        this.nodes = new ArrayList<>();
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket cliente = server.accept();
                    Node node = new Node(cliente);
                    new Thread(node).start();
                    this.nodes.add(node);
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
        int i = 0;

        while (true) {
            System.out.println((++i) + " Round");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (Node node:
                 this.nodes) {
                System.out.println(node);
            }
        }
    }
}
