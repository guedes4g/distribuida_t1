package com.pucrs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Node implements Runnable {
    Socket socket;
    PrintWriter out;
    BufferedReader in;
    public Node(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        System.out.println("Connected: " + this);
        int i = 0;
        while (true) {
            try {
                Thread.sleep(1000);
                this.out.println("" +(i++));
                System.out.println("still: " + this);

            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    @Override
    public String toString() {
        return this.socket.toString() + ", connected:" +this.socket.isConnected();
    }
}
