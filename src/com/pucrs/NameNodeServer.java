package com.pucrs;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NameNodeServer {
    private ServerSocket server;
    private ArrayList<Node> nodes;
    private byte[] bufferMulticastReceiver;
    private byte[] bufferMulticastPublisher;
    private InetAddress group;
    private MulticastSocket socket;

    private HashMap<String, List<FileData>> files;
    
    public NameNodeServer() throws IOException {
        this.server = new ServerSocket(3333);
        this.socket = new MulticastSocket(4446);
        
        //Previne loopback na mesma maquina
        socket.setLoopbackMode(true);

        this.nodes = new ArrayList<>();
        this.bufferMulticastReceiver = new byte[1024 * 4];
        this.group = InetAddress.getByName("224.0.0.1");

        this.files = new HashMap<>();
    }
    
    private void handleNodeConnection() {
        try {
            Socket cliente = server.accept();
            Node node = new Node(cliente);

            //add node into node's list
            this.nodes.add(node);
            
            //get files from node and insert in the hash map 
            files.put(node.getIpAddress(), node.getFiles());
        } catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    public void start() {
        new Thread(() -> {
            while (true)
                this.handleNodeConnection();
        }).start();
//        int i = 0;
//
//        while (true) {
//            System.out.println((++i) + " Round");
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            for (Node node:
//                 this.nodes) {
//                System.out.println(node);
//            }
//        }
/*
        new Thread(() -> {
            try {
                multicastReceiver();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            multicastSelf();
        }).start();*/


    }


    public void multicastReceiver() throws IOException {
        socket.joinGroup(group);
        while (true) {
            DatagramPacket packet = new DatagramPacket(bufferMulticastReceiver, bufferMulticastReceiver.length);
            socket.receive(packet);

            ByteArrayInputStream bais = new ByteArrayInputStream(bufferMulticastReceiver);
            ObjectInputStream ois = new ObjectInputStream(bais);

            String received = new String(
                    packet.getData(), 0, packet.getLength());

            System.out.print("RECEIVED MULTICAST: " + received);
            System.out.println("MULTICAST: " + packet.getAddress());
            if ("end".equals(received)) {
                break;
            }
        }
//        socket.leaveGroup(group);
//        socket.close();
    }

    //TESTE
    public void multicastSelf() {
        while (true) {
            try {
                try {Thread.sleep(1000);} catch (Exception e) {}
                System.out.println("SENT MULTICAST: " + InetAddress.getLocalHost().getHostAddress()+":"+server.getLocalPort());
                multicast(NetworkInterface.getByIndex(1)+":"+server.getLocalPort());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void multicast(Object multicastMessage) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(multicastMessage);
        byte[] data = baos.toByteArray();

        DatagramPacket packet = 
            new DatagramPacket(data, data.length, group, 4446);

        socket.send(packet);
    }

}
