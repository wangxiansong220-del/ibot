package com.bbu.ibot.model.vo;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeRetrievalResult {

    private String knowledgeBase;
    private String namespace;
    private String promptContext;
    private List<RetrievedSegment> segments = new ArrayList<>();

    public KnowledgeRetrievalResult() {
    }

    public KnowledgeRetrievalResult(String knowledgeBase, String namespace, String promptContext, List<RetrievedSegment> segments) {
        this.knowledgeBase = knowledgeBase;
        this.namespace = namespace;
        this.promptContext = promptContext;
        this.segments = segments;
    }

    public String getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(String knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getPromptContext() {
        return promptContext;
    }

    public void setPromptContext(String promptContext) {
        this.promptContext = promptContext;
    }

    public List<RetrievedSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<RetrievedSegment> segments) {
        this.segments = segments;
    }
}
