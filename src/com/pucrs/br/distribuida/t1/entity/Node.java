package com.pucrs.br.distribuida.t1.entity;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Node {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ObjectInputStream is;
    private ObjectOutputStream os;
    
    private long lastPingTime = 0;
    
    public Node(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.is = new ObjectInputStream(socket.getInputStream());
        this.os = new ObjectOutputStream(socket.getOutputStream());
        
        this.lastPingTime = System.currentTimeMillis();
    }

    // <MD5, file_name>
    public HashMap<String, String> receiveFileData() throws IOException, ClassNotFoundException {
        return (HashMap<String, String>) is.readObject();
    }
    
    public List<FileData> getFiles() throws IOException, ClassNotFoundException {
        List<FileData> list = new ArrayList<>();
        
        HashMap<String, String> files = receiveFileData();
        
        files.keySet().forEach((md5) -> {
            list.add(new FileData(md5, files.get(md5), getIpAddress()));
        });
        
        return list;
    }
    
    public String getIpAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    @Override
    public String toString() {
        return this.socket.toString() + ", connected:" +this.socket.isConnected();
    }
}
