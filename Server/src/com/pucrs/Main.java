package com.pucrs;


import java.io.IOException;

public class Main {
    public static void main(String args[]) throws IOException {
        new NameNodeServer().start();
    }
}
