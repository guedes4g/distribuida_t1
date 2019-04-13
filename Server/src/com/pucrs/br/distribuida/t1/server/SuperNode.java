package com.pucrs.br.distribuida.t1.server;

import com.pucrs.br.distribuida.t1.dto.Client2Super;
import com.pucrs.br.distribuida.t1.dto.Super2Client;
import com.pucrs.br.distribuida.t1.dto.Super2Super;
import com.pucrs.br.distribuida.t1.entity.FileData;
import com.pucrs.br.distribuida.t1.entity.Node;
import com.pucrs.br.distribuida.t1.helper.MD5;
import com.pucrs.br.distribuida.t1.helper.Terminal;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.org.apache.bcel.internal.generic.RETURN;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.MethodCall;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import org.jgroups.util.UUID;


public class SuperNode extends ReceiverAdapter {
    private ServerSocket server;
    private byte[] bufferMulticastReceiver;
    private byte[] bufferMulticastPublisher;
    private MulticastSocket socket;

    private HashMap<String, Node> nodes;
    private HashMap<String, List<FileData>> files;
    private int nodeIdEnumerator = 1;
    private JChannel channel;
    private RpcDispatcher dispacherRPC;

    public SuperNode(int unicastPort, int multicastPort) throws IOException, Exception {
        Terminal.debug("Initiating unicast socket with nodes on port '"+unicastPort+"'");
        this.server = new ServerSocket(unicastPort);

        Terminal.debug("Initiating multicast socket with supernodes on port '"+multicastPort+"'");
        this.socket = new MulticastSocket(multicastPort);

        //Previne loopback na mesma maquina
        socket.setLoopbackMode(true);

        this.bufferMulticastReceiver = new byte[1024 * 4];

        this.nodes = new HashMap<>();
        this.files = new HashMap<>();
        startJChannel("superNodeGroup");

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
        String id = UUID.randomUUID().toString();
        System.out.println("new node: " + id );
        Node node = new Node(id , socket);

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
    }

    @Override
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view + ", members: " + new_view.size());
    }

    /**
     * Funcao chamada pelo RPC
     * @return
     */
    public Super2Super getFiles(){
        return new Super2Super(1,files);
    }

    private List<HashMap<String, List<FileData>>> requestFiles() throws Exception {
        RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 5000);

        RspList rsp_list = dispacherRPC.callRemoteMethods(
                null,
                new MethodCall("getFiles", new Object[]{}, new Class[]{}),
                opts);

        List<HashMap<String, List<FileData>>> mapfd = (List<HashMap<String, List<FileData>>>) rsp_list.getResults()
                .stream()
                .map((o) -> (((Super2Super) o).getFilesMap()

                        )
                ).collect(Collectors.toList());


        return mapfd;
    }

    private void startJChannel(String group) throws Exception {
        channel = new JChannel();
        channel.setReceiver(this);
        channel.connect(group);
        dispacherRPC = new RpcDispatcher(channel, this);
    }

    private void handleClientTimeoutRemoval() {
        System.out.println("All nodes: "+ nodes);
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

    private class ClientNodeWorker implements Runnable {
        private Node node;
        private HashMap<String, List<FileData>> superNodeFiles;

        public ClientNodeWorker(Node node, HashMap<String, List<FileData>> superNodeFiles) {
            System.out.println("New client node worker" + superNodeFiles + "\n" + node);
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
            ArrayList<FileData> list = this.getListOfNetworkFiles();

            //Encapsulate and send to connected node
            node.send(new Super2Client(1, list));
        }

        private ArrayList<FileData> getListOfNetworkFiles() {
            ArrayList<FileData> list = new ArrayList<>();
            //Create a list containing all files in other servers (removing it's own)
            try {
                List<HashMap<String, List<FileData>>> result = requestFiles();
                System.out.println(result);
                result.forEach(r ->{
                    r.forEach((key, files) -> {
                        if (!this.node.getId().equals(key))
                            list.addAll(files);
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(list);
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
