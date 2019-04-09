package com.pucrs.br.distribuida.t1.server;

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

    public ClientNode(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        
        this.files = new HashMap<>();
        
        
        
        //get files from my computer
        this.registerFiles();
    }

    public void start() {
        try {
            supernode = new Socket(hostname, port);
                    
            BufferedReader reader = new BufferedReader(new InputStreamReader(supernode.getInputStream()));
            PrintWriter writer = new PrintWriter(supernode.getOutputStream(), true);
            
            //send the files to super server
            this.sendRegisteredFiles(supernode);
            
            this.startUI();

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
    
    private void startUI() {
        while (true) {
            System.out.println("Hi! What you want to do?");
            System.out.println("[0] get files list.");
            System.out.println("[99] exit.");
            
            int option = Terminal.getInt();
            
            switch (option) {
                case 1:
                    this.getFilesFromSuperNode();
                case 99:
                    return;
            }
        }
        
    }
    
    private void getFilesFromSuperNode() {
        
    }
    
    private void registerFiles()  {
        File folder = new File("./Server/Folder");
        File[] listOfFiles = folder.listFiles();
        
        if(listOfFiles != null)
            for (File file : listOfFiles)
                if (file.isFile()) {
                    String fileName = file.getName();
                    String md5 = MD5.generate(file.getAbsolutePath());
                    this.files.put(md5, fileName);
                }
    }
    
    private void sendRegisteredFiles(Socket socket) throws IOException {
        ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
        os.writeObject(this.files);
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
