package com.pucrs;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class NameNodeServer {
    private ServerSocket server;
    private ArrayList<Node> nodes;
    private byte[] bufferMulticastReceiver;
    private byte[] bufferMulticastPublisher;
    private InetAddress group;
    private MulticastSocket socket;

    private HashMap<String, ArrayList<FileData>> files;
    public NameNodeServer() throws IOException {
        this.server = new ServerSocket(3333);
        this.socket = new MulticastSocket(4446);
        //Previne loopback na mesma maquina
        socket.setLoopbackMode(true);

        this.nodes = new ArrayList<>();
        this.bufferMulticastReceiver = new byte[1024];
        this.group = InetAddress.getByName("224.0.0.0");

        this.files = new HashMap<>();
    }

    public void start() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket cliente = server.accept();
                    Node node = new Node(cliente);
                    HashMap<String, String> nodeFiles = node.receiveFileData();

                    // Adiciona arquivos
                    String address = socket.getInterface().getHostAddress();
                    ArrayList<FileData> fileDataList = new ArrayList<>();
                    for(String md5: nodeFiles.keySet())
                        fileDataList.add(new FileData(md5, nodeFiles.get(md5), address));
                    files.put(address, fileDataList);

                    System.out.println(files);
                    this.nodes.add(node);
                } catch (IOException | ClassNotFoundException e){
                    e.printStackTrace();
                }
            }
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

        new Thread(() -> {
            try {
                multicastReceiver();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            multicastSelf();
        }).start();


    }


    public void multicastReceiver() throws IOException {
        socket.joinGroup(group);
        while (true) {
            DatagramPacket packet = new DatagramPacket(bufferMulticastReceiver, bufferMulticastReceiver.length);
            socket.receive(packet);
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

    public void multicast(String multicastMessage) throws IOException {
        bufferMulticastPublisher = multicastMessage.getBytes();
        DatagramPacket packet = new DatagramPacket(
                bufferMulticastPublisher,
                bufferMulticastPublisher.length,
                group,
                4446);

        socket.send(packet);
    }

}
