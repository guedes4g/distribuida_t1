package com.pucrs;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class ClientNodeServer {
    HashMap<String, String> files;

    ClientNodeServer() {
        files = new HashMap<>();
    }

    public void start() {
        String hostname = "127.0.0.1";
        int port = 3333;

        try (Socket socket = new Socket(hostname, port)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            
            registerFiles(socket);
//            listenServer(reader);



        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
    private void registerFiles(Socket socket) throws IOException {
        File folder = new File("./Server/Folder");
        File[] listOfFiles = folder.listFiles();
        System.out.println(folder.isDirectory() +" | "+ folder.isFile());
        System.out.println(listOfFiles);
        if(listOfFiles != null)
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    String fileName = listOfFiles[i].getName();
                    String md5 = generateMD5(fileName);
                    files.put(md5, fileName);
                }
            }

        ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
        os.writeObject(files);
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

    public String generateMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        catch (NoSuchAlgorithmException e) {
           e.printStackTrace();
        }
        return null;
    }

}
