package com.bbu.ibot.service.impl;

import com.bbu.ibot.model.dto.DashboardResponse;
import com.bbu.ibot.model.dto.DashboardResponse.ChatStats;
import com.bbu.ibot.model.dto.DashboardResponse.DailyActivity;
import com.bbu.ibot.model.dto.DashboardResponse.KbStats;
import com.bbu.ibot.model.dto.DashboardResponse.KbSummary;
import com.bbu.ibot.model.dto.DashboardResponse.PopularQuery;
import com.bbu.ibot.model.dto.DashboardResponse.SystemStats;
import com.bbu.ibot.model.entity.ChatMemoryDocument;
import com.bbu.ibot.model.entity.ChatMessageRecord;
import com.bbu.ibot.repository.ChatMemoryRepository;
import com.bbu.ibot.service.DashboardService;
import com.bbu.ibot.service.KnowledgeIngestionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final Path storageRoot;
    private final KnowledgeIngestionService knowledgeIngestionService;
    private final ChatMemoryRepository chatMemoryRepository;

    public DashboardServiceImpl(
            @Value("${ibot.knowledge.base.storage-root:./data/knowledge-base}") String storageRoot,
            KnowledgeIngestionService knowledgeIngestionService,
            ChatMemoryRepository chatMemoryRepository) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.knowledgeIngestionService = knowledgeIngestionService;
        this.chatMemoryRepository = chatMemoryRepository;
    }

    @Override
    public DashboardResponse getDashboard() {
        DashboardResponse response = new DashboardResponse();
        response.setKbStats(buildKbStats());
        response.setChatStats(buildChatStats());
        response.setPopularQueries(buildPopularQueries());
        response.setSystemStats(buildSystemStats());
        return response;
    }

    private KbStats buildKbStats() {
        KbStats stats = new KbStats();
        List<KbSummary> summaries = new ArrayList<>();

        Path root = storageRoot;
        if (!Files.exists(root)) {
            stats.setTotalKnowledgeBases(0);
            stats.setTotalDocuments(0);
            stats.setTotalChunks(0);
            stats.setKnowledgeBases(summaries);
            return stats;
        }

        try (Stream<Path> dirs = Files.list(root)) {
            List<Path> kbDirs = dirs.filter(Files::isDirectory).toList();
            stats.setTotalKnowledgeBases(kbDirs.size());

            int totalDocs = 0;
            long totalChunks = 0;

            for (Path kbDir : kbDirs) {
                String kbName = kbDir.getFileName().toString();
                List<Path> documentFiles;
                try (Stream<Path> files = Files.list(kbDir)) {
                    documentFiles = files
                            .filter(Files::isRegularFile)
                            .filter(p -> !p.getFileName().toString().equals(".kb-meta.json"))
                            .toList();
                }

                int docCount = documentFiles.size();
                int chunkCount = 0;
                for (Path doc : documentFiles) {
                    try {
                        var profile = knowledgeIngestionService.inspectDocument(kbName, doc);
                        chunkCount += profile.chunkCount();
                    } catch (Exception ignored) {
                        // Count as 0 chunks for unparseable files
                    }
                }

                KbSummary summary = new KbSummary();
                summary.setName(kbName);
                summary.setDocumentCount(docCount);
                summary.setChunkCount(chunkCount);
                summaries.add(summary);

                totalDocs += docCount;
                totalChunks += chunkCount;
            }

            stats.setTotalDocuments(totalDocs);
            stats.setTotalChunks(totalChunks);
            summaries.sort(Comparator.comparing(KbSummary::getName));
            stats.setKnowledgeBases(summaries);
        } catch (IOException ignored) {
            stats.setTotalKnowledgeBases(0);
            stats.setTotalDocuments(0);
            stats.setTotalChunks(0);
            stats.setKnowledgeBases(summaries);
        }

        return stats;
    }

    private ChatStats buildChatStats() {
        ChatStats stats = new ChatStats();
        List<ChatMemoryDocument> allSessions = chatMemoryRepository.findAll();

        stats.setTotalSessions(allSessions.size());

        int totalMessages = 0;
        Map<LocalDate, int[]> dailyMap = new LinkedHashMap<>();

        // Initialize last 7 days
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            dailyMap.put(today.minusDays(i), new int[]{0, 0});
        }

        for (ChatMemoryDocument session : allSessions) {
            List<ChatMessageRecord> messages = session.getMessages();
            totalMessages += messages.size();

            // Approximate daily activity based on updatedAt
            LocalDate sessionDate = session.getUpdatedAt() != null
                    ? session.getUpdatedAt().toLocalDate()
                    : (session.getCreatedAt() != null ? session.getCreatedAt().toLocalDate() : today);

            int[] dayCount = dailyMap.get(sessionDate);
            if (dayCount != null) {
                dayCount[0]++; // session count
                dayCount[1] += messages.size(); // message count
            }
        }

        stats.setTotalMessages(totalMessages);

        List<DailyActivity> dailyActivities = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> entry : dailyMap.entrySet()) {
            DailyActivity activity = new DailyActivity();
            activity.setDate(entry.getKey().format(DATE_FORMATTER));
            activity.setSessionCount(entry.getValue()[0]);
            activity.setMessageCount(entry.getValue()[1]);
            dailyActivities.add(activity);
        }
        stats.setDailyActivity(dailyActivities);

        return stats;
    }

    private List<PopularQuery> buildPopularQueries() {
        List<ChatMemoryDocument> allSessions = chatMemoryRepository.findAll();
        Map<String, Integer> queryCounts = new HashMap<>();

        // Common Chinese stop words to filter
        var stopWords = java.util.Set.of(
                "你好", "您好", "谢谢", "再见", "hi", "hello", "hey", "bye", "thanks",
                "?", "？", "的", "了", "是", "在", "有", "和", "就", "不", "也", "都", "而",
                "及", "与", "着", "或", "一个", "没有", "我们", "你们", "他们", "这个", "那个",
                "什么", "怎么", "为什么", "如何", "可以", "能", "会", "要", "让", "把", "被",
                "从", "到", "对", "为", "以", "向", "于", "跟", "比"
        );

        for (ChatMemoryDocument session : allSessions) {
            for (ChatMessageRecord msg : session.getMessages()) {
                if ("USER".equals(msg.getRole())) {
                    String text = msg.getText();
                    if (text == null || text.trim().length() < 2) {
                        continue;
                    }
                    String trimmed = text.trim();
                    if (stopWords.contains(trimmed)) {
                        continue;
                    }
                    queryCounts.merge(trimmed, 1, Integer::sum);
                }
            }
        }

        return queryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(entry -> {
                    PopularQuery q = new PopularQuery();
                    q.setQuery(entry.getKey());
                    q.setCount(entry.getValue());
                    return q;
                })
                .toList();
    }

    private SystemStats buildSystemStats() {
        SystemStats stats = new SystemStats();

        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

            stats.setUsedMemoryMb(heapUsage.getUsed() / (1024 * 1024));
            stats.setMaxMemoryMb(heapUsage.getMax() / (1024 * 1024));
        } catch (Exception ignored) {
            stats.setUsedMemoryMb(0);
            stats.setMaxMemoryMb(0);
        }

        stats.setAvailableProcessors(Runtime.getRuntime().availableProcessors());
        stats.setOs(System.getProperty("os.name", "unknown"));
        stats.setJavaVersion(System.getProperty("java.version", "unknown"));

        return stats;
    }
}
