package com.pucrs.br.distribuida.t1.entity;

import com.pucrs.br.distribuida.t1.dto.Client2Super;

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
    private int id;
    
    public Node(int id) throws IOException {
        this.id = id;

        this.updateLastPingTime();
    }

    public void updateSocket(Socket socket) throws IOException {
        this.socket = socket;

        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.is = new ObjectInputStream(socket.getInputStream());
        this.os = new ObjectOutputStream(socket.getOutputStream());
    }

    public int getId() {
        return this.id;
    }

    public void updateLastPingTime() {
        this.lastPingTime = System.currentTimeMillis();
    }

    public Client2Super getRequest() throws IOException, ClassNotFoundException {
        return (Client2Super) is.readObject();
    }
    
    public List<FileData> getFiles(HashMap<String, String> uploadFilesList) {
        List<FileData> list = new ArrayList<>();
        
        uploadFilesList.keySet().forEach((md5) -> {
            list.add(new FileData(md5, uploadFilesList.get(md5), getIpAddress()));
        });
        
        return list;
    }
    
    public String getIpAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    @Override
    public String toString() {
        return "id: " + this.id + ", ip:" + this.getIpAddress();
    }
}
