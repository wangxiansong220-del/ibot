package com.bbu.ibot.model.dto;

import java.util.List;

public class DocumentMetadataRequest {

    private List<String> tags;
    private String folder;

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
