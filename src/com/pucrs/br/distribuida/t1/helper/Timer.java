package com.pucrs.br.distribuida.t1.helper;

import com.pucrs.br.distribuida.t1.entity.Node;

public class Timer {
    private static int TIMEOUT = 10; //IN SECONDS

    private Timer() {

    }

    public static boolean isExpired(Node node) {
        long currentTime = System.currentTimeMillis() / 1000;
        long lastPing = node.getLastPingTime() / 1000;

        return (currentTime - lastPing) >= TIMEOUT;
    }
}
