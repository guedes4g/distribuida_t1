package com.pucrs.br.distribuida.t1.server;

import com.pucrs.br.distribuida.t1.dto.Client2Super;
import com.pucrs.br.distribuida.t1.helper.MD5;
import com.pucrs.br.distribuida.t1.helper.Terminal;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

public class ClientNode {
    private HashMap<String, String> files;
    private String hostname;
    private int port;
    
    private Socket supernode;
    private ObjectOutputStream superNodeOS;

    public ClientNode(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        
        this.files = new HashMap<>();
        
        //get files from my computer
        Terminal.debug("Registering files from my computer.");
        this.registerFiles();
    }

    public void start() {
        try {
            supernode = new Socket(hostname, port);

            this.superNodeOS = new ObjectOutputStream(this.supernode.getOutputStream());
            
            //send the files to super server
            Terminal.debug("Sending files to supernode");
            this.sendRegisteredFiles();

            //start new thread and initiate the pings
            Terminal.debug("Initiating keep alive process");
            this.keepAlive();

            //Start UI
            Terminal.debug("Starting the UI");
            this.startUI();

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    private void keepAlive() {
        new Thread(() -> {
            while (true)
                this.ping();
        }).start();
    }

    private void ping() {
        try {
            Terminal.debug("I'm alive!");
            this.send(new Client2Super(2, null));

            //waits for next keep alive
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startUI() {
        while (true) {
            System.out.println("Hi! What you want to do?");
            System.out.println("[1] get files list from entire network.");
            System.out.println("[98] toggle debug.");
            System.out.println("[99] exit.");
            
            switch (Terminal.getInt()) {
                case 1:
                    this.getFilesFromSuperNode();
                    break;
                case 98:
                    this.toggleDebug();
                    break;
                case 99:
                    return;
            }

            System.out.println();
        }
        
    }
    
    private void registerFiles()  {
        File folder = new File("./client/files");
        File[] listOfFiles = folder.listFiles();
        
        if(listOfFiles != null)
            for (File f : listOfFiles)
                if (f.isFile())
                    this.files.put(MD5.generate(f.getAbsolutePath()), f.getName());
    }

    private void getFilesFromSuperNode() {
        this.send(new Client2Super(3, null));
    }

    private void toggleDebug() {
        System.out.println("ok, debug was toggled.");
        Terminal.toggleDebug();
    }
    
    private void sendRegisteredFiles() {
        this.send(new Client2Super(1, this.files));
    }

    private void send(Client2Super obj) {
        try {
            this.superNodeOS.writeObject(obj);
        }
        catch (IOException ex) {
            Terminal.debug("ClientNode - send - IOException: " + ex.getMessage());
        }
    }

    public void listenServer(BufferedReader reader) {
        while (true) {
            try {
                String in = reader.readLine();
                System.out.println(in);
            } catch (IOException e) {
                System.out.println("I/O error: " + e.getMessage());
            }
        }
    }
}
