package com.pucrs.br.distribuida.t1.server;

import com.pucrs.br.distribuida.t1.dto.Client2Super;
import com.pucrs.br.distribuida.t1.dto.Super2Client;
import com.pucrs.br.distribuida.t1.entity.FileData;
import com.pucrs.br.distribuida.t1.entity.Node;
import com.pucrs.br.distribuida.t1.helper.Terminal;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SuperNode {
    private ServerSocket server;
    private byte[] bufferMulticastReceiver;
    private byte[] bufferMulticastPublisher;
    private InetAddress group;
    private MulticastSocket socket;

    private HashMap<Integer, Node> nodes;
    private HashMap<Integer, List<FileData>> files;
    private int nodeIdEnumerator = 1;
    
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

            //as soon as a new connection has been made, initiate a new thread
            Terminal.debug("Initiate a new 'client node worker' for '"+node+"'");
            new Thread(new ClientNodeWorker(node, this.files)).start();
        }
        catch (IOException e) {
            Terminal.debug("handleClientNodesRequests IOException: " + e.getMessage());
        }
    }

    private Node getNode(Socket socket) throws IOException {
        Node node = new Node(this.nodeIdEnumerator++, socket);

        this.nodes.put(node.getId(), node);

        return node;
    }

    public void start() {
        new Thread(() -> {
            while (true)
                this.handleClientNodesRequests();
        }).start();

        new Thread(() -> {
            while (true)
                this.handleClientTimeoutRemoval();
        }).start();

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

    private void handleClientTimeoutRemoval() {
        try {
            Terminal.debug("timeout removal - get list of expired nodes");
            List<Node> nodes = this.getNodesTimeouted();

            Terminal.debug("timeout removal - removing nodes from my watch");
            this.removeExpiredNodes(nodes);

            Thread.sleep(950);
        }
        catch (InterruptedException e) {
            Terminal.debug("handleClientTimeoutRemoval InterruptedException: " + e.getMessage());
        }
    }

    private void removeExpiredNodes(List<Node> nodes) {
        for (Node node : nodes) {
            Terminal.debug("timeout removal - removing '"+node+"'");

            //removing files associated
            this.files.remove(node.getId());

            //removing from node's list
            this.nodes.remove(node.getId() + "");
        }
    }

    private List<Node> getNodesTimeouted() {
        return this.nodes.values().stream()
            .filter(n -> n.isTimeOuted())
            .collect(Collectors.toList());
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

    private class ClientNodeWorker implements Runnable {
        private Node node;
        private HashMap<Integer, List<FileData>> superNodeFiles;

        public ClientNodeWorker(Node node, HashMap<Integer, List<FileData>> superNodeFiles) {
            this.node = node;
            this.superNodeFiles = superNodeFiles;
        }

        @Override
        public void run() {
            try {
                this.handleClientRequest();

                this.debug("Connection is closed");
            }
            catch (InterruptedException e) {
                Terminal.debug("Node InterruptedException: " + e.getMessage());
            }
            catch (IOException e) {
                Terminal.debug("Node IOException: " + e.getMessage());
            }
            catch (ClassNotFoundException e) {
                Terminal.debug("Node ClassNotFoundException: " + e.getMessage());
            }
        }

        private void handleClientRequest() throws IOException, ClassNotFoundException, InterruptedException {
            //handle this client request
            while (true) {
                this.debug("Going to wait for my next request.");
                Client2Super req = this.node.getRequest();

                //Check if the connection is still alive
                if (req != null) {
                    this.debug("Request received is '" + req.getCommand() + "'.");
                    this.handleCommand(req);

                    //waits quickly
                    Thread.sleep(100);
                }

                //Ends execution when the connection is closed
                else
                    return;
            }
        }

        private void handleCommand(Client2Super req) {
            switch (req.getCommand()) {
                case 1:
                    this.debug("Just sent my updated list of files.");
                    this.handleNodeListOfFiles(req.getUploadFilesList());
                    break;

                case 2: //PING
                    this.debug("Just pinged - keep me alive.");
                    this.node.updateLastPingTime();
                    break;

                case 3:
                    this.debug("Just requested list of files in the network.");
                    this.handleSendListOfFilesInNetwork();
                    break;
            }
        }

        private void handleSendListOfFilesInNetwork() {
            Terminal.debug("Retrieving list of files in network.");
            List<FileData> list = this.getListOfNetworkFiles();

            //Encapsulate and send to connected node
            node.send(new Super2Client(1, list));
        }

        private List<FileData> getListOfNetworkFiles() {
            List<FileData> list = new ArrayList<>();

            //Create a list containing all files in other servers (removing it's own)
            this.superNodeFiles.forEach((key, files) -> {
                if (this.node.getId() != key)
                    list.addAll(files);
            });

            return list;
        }

        private void handleNodeListOfFiles(HashMap<String, String> uploadFilesList) {
            //get files from server and insert in the hash map
            files.put(node.getId(), node.getFiles(uploadFilesList));
        }

        private void debug(String message) {
            Terminal.debug("Client Node '"+this.node+"' :: " + message);
        }
    }
}
