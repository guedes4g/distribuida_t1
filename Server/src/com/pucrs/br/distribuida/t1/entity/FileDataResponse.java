package com.pucrs.br.distribuida.t1.entity;

public class FileDataResponse extends FileData {

    String content;
    public FileDataResponse(FileData fileData, String content) {
        super(fileData.getMd5(), fileData.getName(), fileData.getIp());
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
