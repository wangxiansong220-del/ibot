package com.bbu.ibot.model.dto;

public class ChatCitationResponse {

    private String fileName;
    private String sourcePath;
    private String chunkIndex;
    private double score;
    private String snippet;

    public ChatCitationResponse() {
    }

    public ChatCitationResponse(String fileName, String sourcePath, String chunkIndex, double score, String snippet) {
        this.fileName = fileName;
        this.sourcePath = sourcePath;
        this.chunkIndex = chunkIndex;
        this.score = score;
        this.snippet = snippet;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(String chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}
