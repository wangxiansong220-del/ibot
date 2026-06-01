package com.bbu.ibot.model.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatResponse {

    private String sessionId;
    private String answer;
    private List<ChatCitationResponse> citations = new ArrayList<>();

    public ChatResponse() {
    }

    public ChatResponse(String sessionId, String answer) {
        this.sessionId = sessionId;
        this.answer = answer;
    }

    public ChatResponse(String sessionId, String answer, List<ChatCitationResponse> citations) {
        this.sessionId = sessionId;
        this.answer = answer;
        this.citations = citations;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<ChatCitationResponse> getCitations() {
        return citations;
    }

    public void setCitations(List<ChatCitationResponse> citations) {
        this.citations = citations;
    }
}
