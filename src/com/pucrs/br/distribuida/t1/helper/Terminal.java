package com.pucrs.br.distribuida.t1.helper;

import java.util.Scanner;

public class Terminal {
    public static int getInt() {
        Scanner s = new Scanner(System.in);
        
        return s.nextInt();
    }
    
    public static int getInt(String message) {
        System.out.println(message);
        
        return getInt();
    }
}