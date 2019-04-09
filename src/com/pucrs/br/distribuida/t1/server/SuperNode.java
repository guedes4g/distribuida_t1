package com.pucrs.br.distribuida.t1.server;

import com.pucrs.br.distribuida.t1.dto.Client2Super;
import com.pucrs.br.distribuida.t1.entity.FileData;
import com.pucrs.br.distribuida.t1.entity.Node;
import com.pucrs.br.distribuida.t1.helper.Terminal;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SuperNode {
    private ServerSocket server;
    private byte[] bufferMulticastReceiver;
    private byte[] bufferMulticastPublisher;
    private InetAddress group;
    private MulticastSocket socket;

    private HashMap<String, Node> nodes;
    private HashMap<Integer, List<FileData>> files;
    
    public SuperNode(int unicastPort, int multicastPort) throws IOException {
        Terminal.debug("Initiating unicast socket with nodes on port '"+unicastPort+"'");
        this.server = new ServerSocket(unicastPort);

        Terminal.debug("Initiating multicast socket with supernodes on port '"+multicastPort+"'");
        this.socket = new MulticastSocket(multicastPort);
        
        //Previne loopback na mesma maquina
        socket.setLoopbackMode(true);

        this.bufferMulticastReceiver = new byte[1024 * 4];
        this.group = InetAddress.getByName("224.0.0.1");

        this.nodes = new HashMap<>();
        this.files = new HashMap<>();
    }
    
    private void handleClientNodesRequests() {
        try {
            //accept new connection
            Terminal.debug("Waiting for connections on unicast socket (client nodes).");
            Socket socket = server.accept();

            //get node
            Node node = this.getNode(socket);
            Terminal.debug("A connection has been made with '"+node+"'.");

            //handle this client request
            this.handleClientRequest(node);

        }
        catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Node getNode(Socket socket) throws IOException {
        Node node = null;
        String ipAddress = socket.getInetAddress().getHostAddress();

        if (!this.nodes.containsKey(ipAddress))
            this.nodes.put(ipAddress, new Node(this.nodes.size()));

        //get the node
        node = this.nodes.get(ipAddress);

        //update the socket
        node.updateSocket(socket);


        return node;
    }

    private void handleClientRequest(Node node) throws IOException, ClassNotFoundException {
        Client2Super req = node.getRequest();

        switch (req.getCommand()) {
            case 1:
                Terminal.debug("Node '"+node+"' sent his list of files.");
                this.handleNodeListOfFiles(node, req.getUploadFilesList());
                break;

            case 2: //PING
                Terminal.debug("Ping is being made by '"+node+"'.");
                node.updateLastPingTime();
                break;

            case 3:
                Terminal.debug("Node '"+node+"' is requesting list of files in network.");
                this.handleSendListOfFilesInNetwork(node);
                break;
        }
    }

    private void handleSendListOfFilesInNetwork(Node node) {
        List<FileData> list = new ArrayList<>();

        //Create a list containing all files in other servers (removing it's own)
        this.files.forEach((key, files) -> {
            if (node.getId() != (key))
                list.addAll(files);
        });
    }

    private void handleNodeListOfFiles(Node node, HashMap<String, String> uploadFilesList) {
        //get files from server and insert in the hash map
        files.put(node.getId(), node.getFiles(uploadFilesList));
    }

    public void start() {
        new Thread(() -> {
            while (true)
                this.handleClientNodesRequests();
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
//            for (Node server:
//                 this.nodes) {
//                System.out.println(server);
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
