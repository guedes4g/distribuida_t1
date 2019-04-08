package com.pucrs;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

public class Node {
    Socket socket;
    PrintWriter out;
    BufferedReader in;
    ObjectInputStream is;
    ObjectOutputStream os;
    public Node(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.is = new ObjectInputStream(socket.getInputStream());
        this.os = new ObjectOutputStream(socket.getOutputStream());
    }

    // <MD5, file_name>
    public HashMap<String, String> receiveFileData() throws IOException, ClassNotFoundException {
        return (HashMap<String,String>) is.readObject();
    }



    @Override
    public String toString() {
        return this.socket.toString() + ", connected:" +this.socket.isConnected();
    }
}
