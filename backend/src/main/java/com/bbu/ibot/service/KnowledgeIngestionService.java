package com.bbu.ibot.service;

import com.bbu.ibot.model.vo.KnowledgeBaseIngestionSummary;
import com.bbu.ibot.model.vo.KnowledgeDocumentProfile;

import java.nio.file.Path;
import java.util.List;

public interface KnowledgeIngestionService {

    KnowledgeDocumentProfile inspectDocument(String knowledgeBase, Path file);

    KnowledgeDocumentProfile ingestDocument(String knowledgeBase, Path file);

    void clearKnowledgeBase(String knowledgeBase);

    KnowledgeBaseIngestionSummary rebuildKnowledgeBase(String knowledgeBase, List<Path> files);
}
