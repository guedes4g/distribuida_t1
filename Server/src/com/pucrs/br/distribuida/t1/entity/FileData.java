package com.pucrs.br.distribuida.t1.entity;

import java.io.Serializable;

public class FileData implements Serializable {
    private String md5;
    private String name;
    private String ip;

    public FileData(String md5, String name, String ip) {
        this.md5 = md5;
        this.name = name;
        this.ip = ip;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String toString() {
        return "md5: " + md5 + ", name: " + name + ", ip: " + ip;
    }
}
