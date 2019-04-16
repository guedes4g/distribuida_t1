package com.pucrs.br.distribuida.t1.server;

import com.pucrs.br.distribuida.t1.dto.Client2Super;
import com.pucrs.br.distribuida.t1.dto.NodeCommunication;
import com.pucrs.br.distribuida.t1.dto.Super2Client;
import com.pucrs.br.distribuida.t1.entity.FileData;
import com.pucrs.br.distribuida.t1.entity.FileDataResponse;
import com.pucrs.br.distribuida.t1.helper.MD5;
import com.pucrs.br.distribuida.t1.helper.Terminal;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class ClientNode {
    private HashMap<String, String> files = new HashMap<>();
    private String superNodeHostname;
    private int superNodePort, p2pPort;
    private ServerSocket p2pServer;

    private Socket supernode;
    private ObjectOutputStream superNodeOS;
    private ObjectInputStream superNodeIS;

    private Thread keepAliveThread = null;
    private ClientNode clientNode;

    private String baseDir = "./Server/client/files/";
    private String baseResponseDir = "./response/";
    public ClientNode(String superNodeHostname, int superNodePort, int p2pPort) throws InterruptedException {
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
            this.supernode = new Socket(superNodeHostname, superNodePort);
            this.p2pServer = new ServerSocket(p2pPort);
            //initiate streams
            this.superNodeOS = new ObjectOutputStream(this.supernode.getOutputStream());
            this.superNodeIS = new ObjectInputStream(this.supernode.getInputStream());

            //send the files to super server
            Terminal.debug("Sending files to supernode");
            this.sendRegisteredFiles();

            //start new thread and initiate the pings
            Terminal.debug("Initiating keep alive process");
            this.keepAliveThread.start();

            Terminal.debug("Starting the P2P");
            this.listenP2P();

            //Start UI
            Terminal.debug("Starting the UI");
            this.startUI();


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

    private void startUI() throws IOException {
        try {
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
                        this.superNodeOS.close();
                        this.supernode.close();
                        return;
                }

                System.out.println();
            }
        } catch (Exception e) {
            Terminal.debug(e.getMessage());
        }

    }
    
    private void registerFiles()  {
        File folder = new File(this.baseDir);
        File[] listOfFiles = folder.listFiles();
        
        if(listOfFiles != null)
            for (File f : listOfFiles)
                if (f.isFile())
                    this.files.put(MD5.generate(f.getAbsolutePath()), f.getName());

        System.out.println(this.files);
    }

    private void getFilesFromSuperNode() {
        //ask to the superNode sends the list of files in the network
        Terminal.debug("Asking to superNode the list of files in network.");
        this.send(new Client2Super(3, null));

        Terminal.debug("Waiting for superNode return the list of files in network.");
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
        Thread[] tpool = new Thread[files.size()];
        for(int i =0; i < files.size(); i++){
            FileData file = files.get(i);
            tpool[i] = new Thread(()->{
                try {
                    Socket socket = new Socket(file.getIp(), this.p2pPort);
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    objectOutputStream.writeObject(file);
                    FileDataResponse response = (FileDataResponse) objectInputStream.readObject();
                    System.out.println("Response from: " +file.getIp()+", "+ response);
                    writeFile(response);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
            tpool[i].start();
        }

        for(int i =0; i < tpool.length; i++){
            try {
                tpool[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Arquivos Recebidos");
    }

    private void writeFile(FileDataResponse response) throws IOException {
        new File(this.baseResponseDir).mkdirs();
        FileWriter fileWriter = new FileWriter(this.baseResponseDir + response.getMd5()+"_"+response.getIp()+"_"+response.getName());
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print(response.getContent());
        printWriter.close();
    }

    private void listenP2P(){
        new Thread(() -> {
            while (true) {
                try{
                    Socket client = this.p2pServer.accept();
                    p2pSendResponse(client);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void p2pSendResponse(Socket client) {
        new Thread(()->{
            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
                FileData request = (FileData) objectInputStream.readObject();
                String filePath = this.baseDir + request.getName();
                String content = getFileContet(filePath);
                FileDataResponse response = new FileDataResponse(request, content );
                objectOutputStream.writeObject(response);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    client.close();
                } catch (IOException e) { }
            }
        }).start();
    }

    private String getFileContet(String filePath) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> lines = Files.lines(Paths.get(filePath), StandardCharsets.ISO_8859_1)) {
            lines.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        return contentBuilder.toString();
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
            System.out.println("["+(iterator++)+"] " + file.getName() + ", MD5: " +file.getMd5() + " host: "+ file.getIp());
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
        System.out.println("Sent " + this.files);
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
