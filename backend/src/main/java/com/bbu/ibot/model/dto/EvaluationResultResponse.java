package com.bbu.ibot.model.dto;

import java.util.List;

public class EvaluationResultResponse {
    private String taskId;
    private String status;
    private int totalQuestions;
    private int matchedCount;
    private double avgScore;
    private String knowledgeBase;
    private String message;
    private List<EvalItem> items;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public int getMatchedCount() { return matchedCount; }
    public void setMatchedCount(int matchedCount) { this.matchedCount = matchedCount; }
    public double getAvgScore() { return avgScore; }
    public void setAvgScore(double avgScore) { this.avgScore = avgScore; }
    public String getKnowledgeBase() { return knowledgeBase; }
    public void setKnowledgeBase(String knowledgeBase) { this.knowledgeBase = knowledgeBase; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<EvalItem> getItems() { return items; }
    public void setItems(List<EvalItem> items) { this.items = items; }

    public static class EvalItem {
        private String question;
        private String expectedAnswer;
        private String actualAnswer;
        private double score;
        private boolean retrieved;
        private List<String> retrievedDocs;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getExpectedAnswer() { return expectedAnswer; }
        public void setExpectedAnswer(String expectedAnswer) { this.expectedAnswer = expectedAnswer; }
        public String getActualAnswer() { return actualAnswer; }
        public void setActualAnswer(String actualAnswer) { this.actualAnswer = actualAnswer; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public boolean isRetrieved() { return retrieved; }
        public void setRetrieved(boolean retrieved) { this.retrieved = retrieved; }
        public List<String> getRetri