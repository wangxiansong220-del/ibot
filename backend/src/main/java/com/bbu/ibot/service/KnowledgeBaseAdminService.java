package com.bbu.ibot.service;

import com.bbu.ibot.model.dto.AdminDocumentUploadResponse;
import com.bbu.ibot.model.dto.DocumentMetadataRequest;
import com.bbu.ibot.model.dto.DocumentPreviewResponse;
import com.bbu.ibot.model.dto.EvaluationResultResponse;
import com.bbu.ibot.model.dto.KnowledgeBaseInfoResponse;
import com.bbu.ibot.model.dto.KnowledgeBaseRebuildResponse;
import com.bbu.ibot.model.dto.KnowledgeDocumentResponse;
import com.bbu.ibot.model.dto.RagStatusResponse;
import com.bbu.ibot.model.dto.SeedResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeBaseAdminService {

    AdminDocumentUploadResponse uploadDocuments(String knowledgeBase, MultipartFile[] files);

    List<KnowledgeDocumentResponse> listDocuments(String knowledgeBase, String search, String tag, String folder);

    KnowledgeBaseRebuildResponse rebuildKnowledgeBase(String knowledgeBase);

    KnowledgeBaseRebuildResponse getRebuildTask(String taskId);

    RagStatusResponse getRagStatus(String knowledgeBase);

    String deleteDocument(String knowledgeBase, String documentId);

    // Knowledge base CRUD
    List<KnowledgeBaseInfoResponse> listKnowledgeBases();

    KnowledgeBaseInfoResponse createKnowledgeBase(String name);

    void deleteKnowledgeBase(String name);

    KnowledgeBaseInfoResponse renameKnowledgeBase(String oldName, String newName);

    // Document metadata
    KnowledgeDocumentResponse updateDocumentMetadata(String knowledgeBase, String documentId, DocumentMe