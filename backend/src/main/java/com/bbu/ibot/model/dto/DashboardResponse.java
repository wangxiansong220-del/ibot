package com.bbu.ibot.model.dto;

import java.util.List;

public class DashboardResponse {

    private KbStats kbStats;
    private ChatStats chatStats;
    private List<PopularQuery> popularQueries;
    private SystemStats systemStats;

    public KbStats getKbStats() {
        return kbStats;
    }

    public void setKbStats(KbStats kbStats) {
        this.kbStats = kbStats;
    }

    public ChatStats getChatStats() {
        return chatStats;
    }

    public void setChatStats(ChatStats chatStats) {
        this.chatStats = chatStats;
    }

    public List<PopularQuery> getPopularQueries() {
        return popularQueries;
    }

    public void setPopularQueries(List<PopularQuery> popularQueries) {
        this.popularQueries = popularQueries;
    }

    public SystemStats getSystemStats() {
        return systemStats;
    }

    public void setSystemStats(SystemStats systemStats) {
        this.systemStats = systemStats;
    }

    // ---- Nested classes ----

    public static class KbStats {
        private int totalKnowledgeBases;
        private int totalDocuments;
        private long totalChunks;
        private List<KbSummary> knowledgeBases;

        public int getTotalKnowledgeBases() {
            return totalKnowledgeBases;
        }

        public void setTotalKnowledgeBases(int totalKnowledgeBases) {
            this.totalKnowledgeBases = totalKnowledgeBases;
        }

        public int getTotalDocuments() {
            return totalDocuments;
        }

        public void setTotalDocuments(int totalDocuments) {
            this.totalDocuments = totalDocuments;
        }

        public long getTotalChunks() {
            return totalChunks;
        }

        public void setTotalChunks(long totalChunks) {
            this.totalChunks = totalChunks;
        }

        public List<KbSummary> getKnowledgeBases() {
            return knowledgeBases;
        }

        public void setKnowledgeBases(List<KbSummary> knowledgeBases) {
            this.knowledgeBases = knowledgeBases;
        }
    }

    public static class KbSummary {
        private String name;
        private int documentCount;
        private int chunkCount;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getDocumentCount() {
            return documentCount;
        }

        public void setDocumentCount(int documentCount) {
            this.documentCount = documentCount;
        }

        public int getChunkCount() {
            return chunkCount;
        }

        public void setChunkCount(int chunkCount) {
            this.chunkCount = chunkCount;
        }
    }

    public static class ChatStats {
        private int totalSessions;
        private int totalMessages;
        private List<DailyActivity> dailyActivity;

        public int getTotalSessions() {
            return totalSessions;
        }

        public void setTotalSessions(int totalSessions) {
            this.totalSessions = totalSessions;
        }

        public int getTotalMessages() {
            return totalMessages;
        }

        public void setTotalMessages(int totalMessages) {
            this.totalMessages = totalMessages;
        }

        public List<DailyActivity> getDailyActivity() {
            return dailyActivity;
        }

        public void setDailyActivity(List<DailyActivity> dailyActivity) {
            this.dailyActivity = dailyActivity;
        }
    }

    public static class DailyActivity {
        private String date;
        private int sessionCount;
        private int messageCount;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public int getSessionCount() {
            return sessionCount;
        }

        public void setSessionCount(int sessionCount) {
            this.sessionCount = sessionCount;
        }

        public int getMessageCount() {
            return messageCount;
        }

        public void setMessageCount(int messageCount) {
            this.messageCount = messageCount;
        }
    }

    public static class PopularQuery {
        private String query;
        private int count;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public static class SystemStats {
        private long usedMemoryMb;
        private long maxMemoryMb;
        private int availableProcessors;
        private String os;
        private String javaVersion;

        public long getUsedMemoryMb() {
            return usedMemoryMb;
        }

        public void setUsedMemoryMb(long usedMemoryMb) {
            this.usedMemoryMb = usedMemoryMb;
        }

        public long getMaxMemoryMb() {
            return maxMemoryMb;
        }

        public void setMaxMemoryMb(long maxMemoryMb) {
            this.maxMemoryMb = maxMemoryMb;
        }

        public int getAvailableProcessors() {
            return availableProcessors;
        }

        public void setAvailableProcessors(int availableProcessors) {
            this.availableProcessors = availableProcessors;
        }

        public String getOs() {
            return os;
        }

        public void setOs(String os) {
            this.os = os;
        }

        public String getJavaVersion() {
            return javaVersion;
        }

        public void setJavaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
        }
    }
}
