package com.pucrs.br.distribuida.t1.entity;

import com.pucrs.br.distribuida.t1.dto.Client2Super;
import com.pucrs.br.distribuida.t1.dto.Super2Client;
import com.pucrs.br.distribuida.t1.helper.Terminal;
import com.pucrs.br.distribuida.t1.helper.Timer;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
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
    
    public Node(int id, Socket socket) throws IOException {
        this.id = id;
        this.socket = socket;

        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.is = new ObjectInputStream(socket.getInputStream());
        this.os = new ObjectOutputStream(socket.getOutputStream());

        this.updateLastPingTime();
    }

    public int getId() {
        return this.id;
    }

    public long getLastPingTime() {
        return this.lastPingTime;
    }

    public boolean isTimeOuted() {
        return Timer.isExpired(this);
    }

    public void updateLastPingTime() {
        this.lastPingTime = System.currentTimeMillis();
    }

    public Client2Super getRequest() throws IOException, ClassNotFoundException {
        try {
            return (Client2Super) is.readObject();
        }
        catch (SocketException ex) {
            Terminal.debug("Node - get - Socket exception: " + ex.getMessage());
        }

        return null;
    }

    public void send(Super2Client bag) {
        try {
            os.writeObject(bag);
        }
        catch (IOException ex) {
            Terminal.debug("Node - send - IO Exception: " + ex.getMessage());
        }
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
