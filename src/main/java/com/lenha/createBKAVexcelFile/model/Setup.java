package com.lenha.createBKAVexcelFile.model;

public class Setup {
    private String linkExcelFile = "";
    private String linkExcelFile2 = "";
    private String linkSaveExcelFileDir = "";
    private String linkSave3BCFileDir = "";//xóa

    public String getLinkExcelFile2() {
        return linkExcelFile2;
    }

    public void setLinkExcelFile2(String linkExcelFile2) {
        this.linkExcelFile2 = linkExcelFile2;
    }

    private String link3bcToriaiFile = "";//xóa
    private String lang = "";

    public String getLinkExcelFile() {
        return linkExcelFile;
    }

    public void setLinkExcelFile(String linkExcelFile) {
        this.linkExcelFile = linkExcelFile;
    }

    public String getLink3bcToriaiFile() {
        return link3bcToriaiFile;
    }

    public void setLink3bcToriaiFile(String link3bcToriaiFile) {
        this.link3bcToriaiFile = link3bcToriaiFile;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getLinkSave3BCFileDir() {
        return linkSave3BCFileDir;
    }

    public String getLinkSaveExcelFileDir() {
        return linkSaveExcelFileDir;
    }

    public void setLinkSave3BCFileDir(String linkSave3BCFileDir) {
        this.linkSave3BCFileDir = linkSave3BCFileDir;
    }

    public void setLinkSaveExcelFileDir(String linkSaveExcelFileDir) {
        this.linkSaveExcelFileDir = linkSaveExcelFileDir;
    }


}


