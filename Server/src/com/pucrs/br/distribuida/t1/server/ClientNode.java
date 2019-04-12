package com.pucrs.br.distribuida.t1.server;

import com.pucrs.br.distribuida.t1.dto.Client2Super;
import com.pucrs.br.distribuida.t1.dto.Super2Client;
import com.pucrs.br.distribuida.t1.entity.FileData;
import com.pucrs.br.distribuida.t1.helper.MD5;
import com.pucrs.br.distribuida.t1.helper.Terminal;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClientNode {
    private HashMap<String, String> files = new HashMap<>();
    private String superNodeHostname;
    private int superNodePort, p2pPort;
    
    private Socket supernode;
    private ObjectOutputStream superNodeOS;
    private ObjectInputStream superNodeIS;

    private Thread keepAliveThread = null;

    public ClientNode(String superNodeHostname, int superNodePort, int p2pPort) {
        this.superNodeHostname = superNodeHostname;
        this.superNodePort = superNodePort;
        this.p2pPort = p2pPort;
        
        //get files from my computer
        Terminal.debug("Registering files from my computer.");
        this.registerFiles();

        //keep alive - ping
        Terminal.debug("Registering keep alive thread");
        this.initiateKeepAliveThread();
    }

    public void start() {
        try {
            supernode = new Socket(superNodeHostname, superNodePort);

            //initiate streams
            this.superNodeOS = new ObjectOutputStream(this.supernode.getOutputStream());
            this.superNodeIS = new ObjectInputStream(this.supernode.getInputStream());
            
            //send the files to super server
            Terminal.debug("Sending files to supernode");
            this.sendRegisteredFiles();

            //start new thread and initiate the pings
            Terminal.debug("Initiating keep alive process");
            this.keepAliveThread.start();

            //Start UI
            Terminal.debug("Starting the UI");
            this.startUI();

            //Ends connection
            this.superNodeOS.close();
            this.supernode.close();

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }

    private void initiateKeepAliveThread() {
        this.keepAliveThread = new Thread(() -> {
            while (true)
                this.ping();
        });
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
        //ask to the superNode sends the list of files in the network
        Terminal.debug("Asking to superNode the list of files in network.");
        this.send(new Client2Super(3, null));

        Terminal.debug("Waiting for supeNode return the list of files in network.");
        List<FileData> filesFromNetwork = this.waitForListOfFilesFromNetwork();

        if (filesFromNetwork != null) {
            Terminal.debug("Returned '"+filesFromNetwork.size()+"' files from network");

            this.printListOfNetworkFiles(filesFromNetwork);

            this.askListOfFilesToDownload(filesFromNetwork);
        }
        else
            System.out.println("An error has been ocurred while waiting for the list of files from network. Supernode returned with an unknown bag.");
    }

    private void askListOfFilesToDownload(List<FileData> filesFromNetwork) {
        System.out.println("Insert here the files you want to download separating with commas...");
        System.out.println("Insert -1 to exit.");

        //wait for user's input
        String userInput = Terminal.getString();

        //It means the user really wants to download
        if (!userInput.equals("-1")) {
            List<FileData> files = this.getNetworkFilesBasedOnUserInput(userInput, filesFromNetwork);
            
            this.initiateP2P(files);
        }
    }

    private void initiateP2P(List<FileData> files) {

    }

    private List<FileData> getNetworkFilesBasedOnUserInput(String userInput, List<FileData> filesFromNetwork) {
        String[] tokens = userInput.split(",");

        List<FileData> files = new ArrayList<>();
        FileData f = null;
        int index = 0;

        //Iterate all user's tokens
        for (String token : tokens) {
            try {
                //try to parse the index
                index = Integer.parseInt(token);

                //try to retrieve the object based on index (it may throw an exception)
                f = filesFromNetwork.get(index);

                //in case it worked, add into returned list
                files.add(f);
            }
            catch (Exception ex) {}
        }

        return files;
    }

    private void printListOfNetworkFiles(List<FileData> filesFromNetwork) {
        int iterator = 0;

        System.out.println("\nHere's the network list of files.");

        for (FileData file : filesFromNetwork)
            System.out.println("["+(iterator++)+"] " + file.getName());
    }

    private List<FileData> waitForListOfFilesFromNetwork() {
        try {
            Super2Client bag = (Super2Client) this.superNodeIS.readObject();

            return bag.getFilesList();
        }
        catch (IOException e) {
            Terminal.debug("waitForListOfFilesFromNetwork - IOException: " + e.getMessage());
        }
        catch (ClassNotFoundException e) {
            Terminal.debug("waitForListOfFilesFromNetwork - ClassNotFound: " + e.getMessage());
        }

        return null;
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
