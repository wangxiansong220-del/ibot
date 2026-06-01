package com.bbu.ibot.service.impl;

import com.bbu.ibot.model.dto.AdminDocumentUploadResponse;
import com.bbu.ibot.model.dto.DocumentMetadataRequest;
import com.bbu.ibot.model.dto.KnowledgeBaseInfoResponse;
import com.bbu.ibot.model.dto.KnowledgeBaseRebuildDocumentResponse;
import com.bbu.ibot.model.dto.KnowledgeBaseRebuildResponse;
import com.bbu.ibot.model.dto.KnowledgeDocumentResponse;
import com.bbu.ibot.model.dto.SeedResponse;
import com.bbu.ibot.model.dto.DocumentPreviewResponse;
import com.bbu.ibot.model.dto.EvaluationResultResponse;
import com.bbu.ibot.model.dto.RagStatusResponse;
import com.bbu.ibot.model.entity.AdminTaskRecord;
import com.bbu.ibot.model.vo.KnowledgeDocumentProfile;
import com.bbu.ibot.service.AdminTaskService;
import com.bbu.ibot.service.KnowledgeBaseAdminService;
import com.bbu.ibot.service.KnowledgeIngestionService;
import com.bbu.ibot.service.KnowledgeRetrievalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class KnowledgeBaseAdminServiceImpl implements KnowledgeBaseAdminService {

    private static final DateTimeFormatter FILE_NAME_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final String STATUS_SYNC_FAILED = "PINECONE_SYNC_FAILED";
    private static final String META_FILE_NAME = ".kb-meta.json";

    private static final TypeReference<Map<String, Object>> META_TYPE_REF = new TypeReference<>() {};

    private final Path storageRoot;
    private final boolean pineconeEnabled;
    private final String pineconeApiKey;
    private final String pineconeIndexName;
    private final String pineconeNamespace;
    private final String embeddingProvider;
    private final int chunkSize;
    private final int chunkOverlap;
    private final KnowledgeIngestionService knowledgeIngestionService;
    private final AdminTaskService adminTaskService;
    private final ObjectMapper objectMapper;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final Map<String, KnowledgeBaseRebuildResponse> rebuildTasks = new ConcurrentHashMap<>();

    @Autowired
    public KnowledgeBaseAdminServiceImpl(
            @Value("${ibot.knowledge.base.storage-root:./data/knowledge-base}") String storageRoot,
            @Value("${ibot.rag.pinecone.enabled:false}") boolean pineconeEnabled,
            @Value("${ibot.rag.pinecone.api-key:}") String pineconeApiKey,
            @Value("${ibot.rag.pinecone.index-name:}") String pineconeIndexName,
            @Value("${ibot.rag.pinecone.namespace:default}") String pineconeNamespace,
            @Value("${ibot.rag.embedding-provider:reserved}") String embeddingProvider,
            @Value("${ibot.rag.chunk-size:800}") int chunkSize,
            @Value("${ibot.rag.chunk-overlap:120}") int chunkOverlap,
            KnowledgeIngestionService knowledgeIngestionService,
            AdminTaskService adminTaskService,
            ObjectMapper objectMapper,
                                        KnowledgeRetrievalService knowledgeRetrievalService) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.pineconeEnabled = pineconeEnabled;
        this.pineconeApiKey = pineconeApiKey;
        this.pineconeIndexName = pineconeIndexName;
        this.pineconeNamespace = pineconeNamespace;
        this.embeddingProvider = embeddingProvider;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.knowledgeIngestionService = knowledgeIngestionService;
        this.adminTaskService = adminTaskService;
        this.objectMapper = objectMapper;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    public KnowledgeBaseAdminServiceImpl(String storageRoot,
                                         boolean pineconeEnabled,
                                         String pineconeApiKey,
                                         String pineconeIndexName,
                                         String pineconeNamespace,
                                         String embeddingProvider,
                                         int chunkSize,
                                         int chunkOverlap,
                                         KnowledgeIngestionService knowledgeIngestionService) {
        this(storageRoot, pineconeEnabled, pineconeApiKey, pineconeIndexName, pineconeNamespace, embeddingProvider,
                chunkSize, chunkOverlap, knowledgeIngestionService, new NoopAdminTaskService(), new ObjectMapper(), null);
    }

    @Override
    public AdminDocumentUploadResponse uploadDocuments(String knowledgeBase, MultipartFile[] files) {
        String normalizedKnowledgeBase = normalizeKnowledgeBase(knowledgeBase);
        if (files == null || files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "files cannot be empty");
        }

        AdminTaskRecord task = adminTaskService.createTask("KNOWLEDGE_UPLOAD", "KNOWLEDGE_BASE", normalizedKnowledgeBase, "Uploading knowledge base documents");
        try {
            Path knowledgeBaseDirectory = ensureKnowledgeBaseDirectory(normalizedKnowledgeBase);
            List<KnowledgeDocumentResponse> uploadedDocuments = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String originalFileName = StringUtils.hasText(file.getOriginalFilename())
                        ? Path.of(file.getOriginalFilename()).getFileName().toString()
                        : "document.bin";
                String storedFileName = FILE_NAME_TIME_FORMATTER.format(LocalDateTime.now()) + "-" + originalFileName;
                Path target = knowledgeBaseDirectory.resolve(storedFileName);

                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException exception) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "failed to store file: " + originalFileName, exception);
                }

                // Only inspect, don't ingest during batch upload (ingestion happens during rebuild)
                KnowledgeDocumentProfile profile = knowledgeIngestionService.inspectDocument(normalizedKnowledgeBase, target);
                uploadedDocuments.add(toDocumentResponse(normalizedKnowledgeBase, target, profile));
            }

            if (uploadedDocuments.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "files cannot be empty");
            }

            AdminDocumentUploadResponse response = new AdminDocumentUploadResponse();
            response.setKnowledgeBase(normalizedKnowledgeBase);
            response.setUploadedCount(uploadedDocuments.size());
            response.setDocuments(uploadedDocuments);
            String message = warnings.isEmpty()
                    ? "Uploaded " + uploadedDocuments.size() + " document(s)"
                    : "Uploaded " + uploadedDocuments.size() + " document(s) with warnings: " + String.join("; ", warnings);
            response.setMessage(message);
            adminTaskService.updateTask(task.getId(), warnings.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_WARNING", 100,
                    message, toJson(uploadedDocuments));
            return response;
        } catch (RuntimeException exception) {
            adminTaskService.updateTask(task.getId(), "FAILED", 100,
                    exception.getMessage() == null ? "knowledge upload failed" : exception.getMessage(), null);
            throw exception;
        }
    }

    @Override
    public List<KnowledgeDocumentResponse> listDocuments(String knowledgeBase, String search, String tag, String folder) {
        String normalizedKnowledgeBase = normalizeKnowledgeBase(knowledgeBase);
        Path knowledgeBaseDirectory = storageRoot.resolve(normalizedKnowledgeBase);
        if (!Files.exists(knowledgeBaseDirectory)) {
            return List.of();
        }

        List<KnowledgeDocumentResponse> documents;
        try (Stream<Path> stream = Files.list(knowledgeBaseDirectory)) {
            documents = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals(META_FILE_NAME))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .map(path -> toDocumentResponse(normalizedKnowledgeBase, path,
                            knowledgeIngestionService.inspectDocument(normalizedKnowledgeBase, path)))
                    .toList();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to list knowledge-base documents", exception);
        }

        // Enrich with tags and folder from metadata
        Map<String, Map<String, Object>> metaMap = loadDocumentMeta(normalizedKnowledgeBase);
        documents = documents.stream().map(doc -> {
            String docId = doc.getDocumentId();
            if (metaMap.containsKey(docId)) {
                Map<String, Object> meta = metaMap.get(docId);
                @SuppressWarnings("unchecked")
                List<String> tags = meta.containsKey("tags") ? (List<String>) meta.get("tags") : List.of();
                String folderVal = meta.containsKey("folder") ? (String) meta.get("folder") : "";
                doc.setTags(tags);
                doc.setFolder(folderVal);
            } else {
                doc.setTags(List.of());
                doc.setFolder("");
            }
            return doc;
        }).toList();

        // Apply filters
        Stream<KnowledgeDocumentResponse> docStream = documents.stream();

        if (StringUtils.hasText(search)) {
            String lowerSearch = search.toLowerCase();
            docStream = docStream.filter(doc -> doc.getFileName().toLowerCase().contains(lowerSearch));
        }

        if (StringUtils.hasText(tag) && !"__all__".equals(tag)) {
            String lowerTag = tag.toLowerCase();
            docStream = docStream.filter(doc ->
                    doc.getTags() != null && doc.getTags().stream().anyMatch(t -> t.toLowerCase().equals(lowerTag)));
        }

        if (StringUtils.hasText(folder) && !"__all__".equals(folder)) {
            docStream = docStream.filter(doc -> {
                String docFolder = doc.getFolder();
                if (!StringUtils.hasText(docFolder)) {
                    return "__none__".equals(folder);
                }
                return docFolder.equals(folder);
            });
        }

        return docStream.toList();
    }

    @Override
    public String deleteDocument(String knowledgeBase, String documentId) {
        String normalizedKnowledgeBase = normalizeKnowledgeBase(knowledgeBase);
        KnowledgeDocumentResponse targetDocument = listDocuments(normalizedKnowledgeBase, null, null, null).stream()
                .filter(document -> document.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "document not found"));

        AdminTaskRecord task = adminTaskService.createTask("KNOWLEDGE_DELETE", "DOCUMENT", targetDocument.getFileName(), "Deleting knowledge base document");
        try {
            Files.deleteIfExists(Path.of(targetDocument.getStoragePath()));
            // Clean up metadata
            removeDocumentMeta(normalizedKnowledgeBase, documentId);
            if (pineconeEnabled && hasPineconeConfig()) {
                List<Path> remainingFiles = listDocuments(normalizedKnowledgeBase, null, null, null).stream()
                        .map(document -> Path.of(document.getStoragePath()))
                        .toList();
                try {
                    knowledgeIngestionService.clearKnowledgeBase(normalizedKnowledgeBase);
                    if (!remainingFiles.isEmpty()) {
                        knowledgeIngestionService.rebuildKnowledgeBase(normalizedKnowledgeBase, remainingFiles);
                    }
                } catch (RuntimeException exception) {
                    String message = "document deleted locally, but vector index sync failed: " + sanitizeMessage(exception);
                    adminTaskService.updateTask(task.getId(), "COMPLETED_WITH_WARNING", 100, message, null);
                    return message;
                }
            }
            adminTaskService.updateTask(task.getId(), "COMPLETED", 100, "document deleted", null);
            return "document deleted";
        } catch (IOException exception) {
            adminTaskService.updateTask(task.getId(), "FAILED", 100, exception.getMessage(), null);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to delete document", exception);
        }
    }

    @Override
    public KnowledgeBaseRebuildResponse rebuildKnowledgeBase(String knowledgeBase) {
        String normalizedKnowledgeBase = normalizeKnowledgeBase(knowledgeBase);
        List<KnowledgeDocumentResponse> documents = listDocuments(normalizedKnowledgeBase, null, null, null);
        AdminTaskRecord adminTaskRecord = adminTaskService.createTask("KNOWLEDGE_REBUILD", "KNOWLEDGE_BASE", normalizedKnowledgeBase, "Knowledge base rebuild accepted");

        KnowledgeBaseRebuildResponse response = new KnowledgeBaseRebuildResponse();
        response.setTaskId(String.valueOf(adminTaskRecord.getId()));
        response.setKnowledgeBase(normalizedKnowledgeBase);
        response.setNamespace(normalizedKnowledgeBase);
        response.setStatus("PENDING");
        response.setProgress(0);
        response.setDocumentCount(documents.size());
        response.setProcessedDocuments(0);
        response.setChunkCount(0);
        response.setIndexingStatus("PENDING");
        response.setTriggeredAt(LocalDateTime.now());
        response.setMessage("Rebuild task accepted");
        rebuildTasks.put(response.getTaskId(), response);

        if (documents.isEmpty() || !pineconeEnabled || !hasPineconeConfig()) {
            response.setStatus("COMPLETED");
            response.setProgress(100);
            response.setIndexingStatus(resolveRetrievalStatus(documents.size()));
            response.setCompletedAt(LocalDateTime.now());
            response.setMessage("No asynchronous rebuild was required");
            adminTaskService.updateTask(adminTaskRecord.getId(), "COMPLETED", 100, response.getMessage(), toJson(response));
            return response;
        }

        CompletableFuture.runAsync(() -> executeRebuildTask(response.getTaskId(), normalizedKnowledgeBase, documents));
        return response;
    }

    @Override
    public KnowledgeBaseRebuildResponse getRebuildTask(String taskId) {
        KnowledgeBaseRebuildResponse response = rebuildTasks.get(taskId);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "rebuild task not found");
        }
        return response;
    }

    @Override
    public RagStatusResponse getRagStatus(String knowledgeBase) {
        String normalizedKnowledgeBase = normalizeKnowledgeBase(knowledgeBase);
        List<KnowledgeDocumentResponse> documents = listDocuments(normalizedKnowledgeBase, null, null, null);

        RagStatusResponse response = new RagStatusResponse();
        response.setKnowledgeBase(normalizedKnowledgeBase);
        response.setStorageRoot(storageRoot.resolve(normalizedKnowledgeBase).toString());
        response.setDocumentCount(documents.size());
        response.setRagEnabled(pineconeEnabled && hasPineconeConfig());
        response.setRetrievalStatus(resolveRetrievalStatus(documents.size()));
        response.setEmbeddingProvider(embeddingProvider);
        response.setVectorStore("pinecone");
        response.setChunkSize(chunkSize);
        response.setChunkOverlap(chunkOverlap);
        return response;
    }

    // ========== Knowledge Base CRUD ==========

    @Override
    public List<KnowledgeBaseInfoResponse> listKnowledgeBases() {
        try {
            if (!Files.exists(storageRoot)) {
                return List.of();
            }

            try (Stream<Path> stream = Files.list(storageRoot)) {
                return stream
                        .filter(Files::isDirectory)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .map(this::toKnowledgeBaseInfo)
                        .toList();
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to list knowledge bases", exception);
        }
    }

    @Override
    public KnowledgeBaseInfoResponse createKnowledgeBase(String name) {
        String normalizedName = normalizeKnowledgeBase(name);
        Path kbDirectory = storageRoot.resolve(normalizedName);

        if (Files.exists(kbDirectory)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "knowledge base '" + normalizedName + "' already exists");
        }

        try {
            Files.createDirectories(kbDirectory);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to create knowledge base directory", exception);
        }

        return toKnowledgeBaseInfo(kbDirectory);
    }

    @Override
    public void deleteKnowledgeBase(String name) {
        String normalizedName = normalizeKnowledgeBase(name);
        Path kbDirectory = storageRoot.resolve(normalizedName);

        if (!Files.exists(kbDirectory)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "knowledge base '" + normalizedName + "' not found");
        }

        try {
            // Clear Pinecone if configured
            if (pineconeEnabled && hasPineconeConfig()) {
                try {
                    knowledgeIngestionService.clearKnowledgeBase(normalizedName);
                } catch (RuntimeException exception) {
                    // Log but continue with local deletion
                }
            }

            // Delete all files in the directory
            try (Stream<Path> files = Files.walk(kbDirectory)) {
                files.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to delete knowledge base", exception);
        }
    }

    @Override
    public KnowledgeBaseInfoResponse renameKnowledgeBase(String oldName, String newName) {
        String normalizedOld = normalizeKnowledgeBase(oldName);
        String normalizedNew = normalizeKnowledgeBase(newName);

        if (normalizedOld.equals(normalizedNew)) {
            // Same name, just return info
            return toKnowledgeBaseInfo(storageRoot.resolve(normalizedOld));
        }

        Path oldPath = storageRoot.resolve(normalizedOld);
        Path newPath = storageRoot.resolve(normalizedNew);

        if (!Files.exists(oldPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "knowledge base '" + normalizedOld + "' not found");
        }

        if (Files.exists(newPath)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "knowledge base '" + normalizedNew + "' already exists");
        }

        try {
            Files.move(oldPath, newPath);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to rename knowledge base", exception);
        }

        return toKnowledgeBaseInfo(newPath);
    }

    // ========== Document Metadata ==========

    @Override
    public KnowledgeDocumentResponse updateDocumentMetadata(String knowledgeBase, String documentId, DocumentMetadataRequest request) {
        String normalizedKnowledgeBase = normalizeKnowledgeBase(knowledgeBase);
        // Verify document exists
        KnowledgeDocumentResponse doc = listDocuments(normalizedKnowledgeBase, null, null, null).stream()
                .filter(d -> d.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "document not found"));

        Map<String, Map<String, Object>> metaMap = loadDocumentMeta(normalizedKnowledgeBase);
        Map<String, Object> docMeta = metaMap.computeIfAbsent(documentId, k -> new LinkedHashMap<>());

        if (request.getTags() != null) {
            docMeta.put("tags", new ArrayList<>(request.getTags()));
        }
        if (request.getFolder() != null) {
            if (request.getFolder().isBlank()) {
                docMeta.put("folder", "");
            } else {
                docMeta.put("folder", request.getFolder().replaceAll("^/+", "").replaceAll("/+$", ""));
            }
        }

        saveDocumentMeta(normalizedKnowledgeBase, metaMap);

        // Return updated document
        KnowledgeDocumentResponse updated = listDocuments(normalizedKnowledgeBase, null, null, null).stream()
                .filter(d -> d.getDocumentId().equals(documentId))
                .findFirst()
                .orElse(doc);
        return updated;
    }

    @Override
    public List<String> listKnowledgeBaseTags(String knowledgeBase) {
        String normalizedKnowledgeBase = normalizeKnowledgeBase(knowledgeBase);
        Map<String, Map<String, Object>> metaMap = loadDocumentMeta(normalizedKnowledgeBase);
        Set<String> allTags = new HashSet<>();
        for (Map<String, Object> docMeta : metaMap.values()) {
            Object tagsObj = docMeta.get("tags");
            if (tagsObj instanceof List<?> tagList) {
                for (Object t : tagList) {
                    if (t instanceof String tag && StringUtils.hasText(tag)) {
                        allTags.add(tag);
                    }
                }
            }
        }
        return allTags.stream().sorted().toList();
    }

    @Override
    public List<String> listKnowledgeBaseFolders(String knowledgeBase) {
        String normalizedKnowledgeBase = normalizeKnowledgeBase(knowledgeBase);
        Map<String, Map<String, Object>> metaMap = loadDocumentMeta(normalizedKnowledgeBase);
        Set<String> allFolders = new HashSet<>();
        for (Map<String, Object> docMeta : metaMap.values()) {
            Object folderObj = docMeta.get("folder");
            if (folderObj instanceof String folder && StringUtils.hasText(folder)) {
                allFolders.add(folder);
            }
        }
        return allFolders.stream().sorted().toList();
    }

    // ========== Metadata Persistence ==========

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> loadDocumentMeta(String knowledgeBase) {
        Path metaFile = storageRoot.resolve(knowledgeBase).resolve(META_FILE_NAME);
        if (!Files.exists(metaFile)) {
            return new LinkedHashMap<>();
        }
        try {
            String content = Files.readString(metaFile, StandardCharsets.UTF_8);
            Map<String, Object> root = objectMapper.readValue(content, META_TYPE_REF);
            Object documentsObj = root.get("documents");
            if (documentsObj instanceof Map<?, ?> docMap) {
                Map<String, Map<String, Object>> result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : docMap.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof Map<?, ?> val) {
                        Map<String, Object> stringVal = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> ve : val.entrySet()) {
                            if (ve.getKey() instanceof String vk) {
                                stringVal.put(vk, ve.getValue());
                            }
                        }
                        result.put(key, stringVal);
                    }
                }
                return result;
            }
            return new LinkedHashMap<>();
        } catch (IOException exception) {
            return new LinkedHashMap<>();
        }
    }

    private void saveDocumentMeta(String knowledgeBase, Map<String, Map<String, Object>> documents) {
        Path metaFile = storageRoot.resolve(knowledgeBase).resolve(META_FILE_NAME);
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("documents", documents);
            String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(metaFile, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to save document metadata", exception);
        }
    }

    private void removeDocumentMeta(String knowledgeBase, String documentId) {
        Map<String, Map<String, Object>> metaMap = loadDocumentMeta(knowledgeBase);
        if (metaMap.remove(documentId) != null) {
            saveDocumentMeta(knowledgeBase, metaMap);
        }
    }

    // ========== Helper Methods ==========

    private KnowledgeBaseInfoResponse toKnowledgeBaseInfo(Path kbDirectory) {
        KnowledgeBaseInfoResponse info = new KnowledgeBaseInfoResponse();
        info.setName(kbDirectory.getFileName().toString());

        try {
            // Count document files (excluding metadata)
            try (Stream<Path> files = Files.list(kbDirectory)) {
                int docCount = (int) files
                        .filter(Files::isRegularFile)
                        .filter(path -> !path.getFileName().toString().equals(META_FILE_NAME))
                        .count();
                info.setDocumentCount(docCount);
            }

            // Use directory create time as a proxy for created time
            info.setCreatedAt(Files.getLastModifiedTime(kbDirectory).toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime());

            // Find the most recent file modification time
            try (Stream<Path> files = Files.list(kbDirectory)) {
                files.filter(Files::isRegularFile)
                        .max(Comparator.comparing(path -> {
                            try {
                                return Files.getLastModifiedTime(path);
                            } catch (IOException e) {
                                return java.nio.file.attribute.FileTime.fromMillis(0);
                            }
                        }))
                        .ifPresentOrElse(
                                latest -> {
                                    try {
                                        info.setUpdatedAt(Files.getLastModifiedTime(latest).toInstant()
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toLocalDateTime());
                                    } catch (IOException ignored) {
                                        info.setUpdatedAt(info.getCreatedAt());
                                    }
                                },
                                () -> info.setUpdatedAt(info.getCreatedAt())
                        );
            }
        } catch (IOException exception) {
            info.setDocumentCount(0);
            info.setCreatedAt(LocalDateTime.now());
            info.setUpdatedAt(LocalDateTime.now());
        }

        return info;
    }

    private Path ensureKnowledgeBaseDirectory(String knowledgeBase) {
        try {
            Path directory = storageRoot.resolve(knowledgeBase);
            Files.createDirectories(directory);
            return directory;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to prepare knowledge-base directory", exception);
        }
    }

    private String normalizeKnowledgeBase(String knowledgeBase) {
        if (!StringUtils.hasText(knowledgeBase)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "knowledgeBase cannot be blank");
        }
        return knowledgeBase.trim().toLowerCase().replaceAll("[^a-z0-9-_]", "-");
    }

    private KnowledgeDocumentResponse toDocumentResponse(String knowledgeBase, Path path, KnowledgeDocumentProfile profile) {
        try {
            KnowledgeDocumentResponse response = new KnowledgeDocumentResponse();
            response.setDocumentId(UUID.nameUUIDFromBytes(path.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8)).toString());
            response.setKnowledgeBase(knowledgeBase);
            response.setNamespace(profile.namespace());
            response.setFileName(path.getFileName().toString());
            response.setFileSize(Files.size(path));
            response.setChunkCount(profile.chunkCount());
            response.setStoragePath(path.toAbsolutePath().toString());
            response.setIndexingStatus(profile.indexingStatus());
            response.setUploadedAt(Files.getLastModifiedTime(path).toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime());
            return response;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to read knowledge-base document metadata", exception);
        }
    }

    private String resolveRetrievalStatus(int documentCount) {
        if (!pineconeEnabled) {
            return "PINECONE_DISABLED";
        }
        if (!hasPineconeConfig()) {
            return "PINECONE_CONFIG_REQUIRED";
        }
        if (documentCount == 0) {
            return "NO_DOCUMENTS";
        }
        return "PINECONE_READY_FOR_INGESTION";
    }

    private KnowledgeDocumentProfile inspectOrIngestDocument(String knowledgeBase, Path target, List<String> warnings) {
        if (!pineconeEnabled || !hasPineconeConfig()) {
            return knowledgeIngestionService.inspectDocument(knowledgeBase, target);
        }

        try {
            return knowledgeIngestionService.ingestDocument(knowledgeBase, target);
        } catch (RuntimeException exception) {
            warnings.add(target.getFileName() + " sync failed: " + sanitizeMessage(exception));
            KnowledgeDocumentProfile profile = knowledgeIngestionService.inspectDocument(knowledgeBase, target);
            return new KnowledgeDocumentProfile(profile.namespace(), profile.chunkCount(), STATUS_SYNC_FAILED);
        }
    }

    private String sanitizeMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (!StringUtils.hasText(message)) {
            message = exception.getMessage();
        }
        return StringUtils.hasText(message) ? message : exception.getClass().getSimpleName();
    }

    private boolean hasPineconeConfig() {
        return StringUtils.hasText(pineconeApiKey)
                && StringUtils.hasText(pineconeIndexName)
                && StringUtils.hasText(pineconeNamespace);
    }

    private void executeRebuildTask(String taskId, String knowledgeBase, List<KnowledgeDocumentResponse> documents) {
        KnowledgeBaseRebuildResponse task = rebuildTasks.get(taskId);
        if (task == null) {
            return;
        }
        long adminTaskId = Long.parseLong(taskId);

        try {
            task.setStatus("RUNNING");
            task.setIndexingStatus("PINECONE_REBUILD_RUNNING");
            task.setMessage("Clearing namespace and re-indexing documents");
            task.setDocuments(new ArrayList<>());
            adminTaskService.updateTask(adminTaskId, "RUNNING", 5, task.getMessage(), null);

            knowledgeIngestionService.clearKnowledgeBase(knowledgeBase);

            int processedDocuments = 0;
            int totalChunks = 0;
            for (KnowledgeDocumentResponse document : documents) {
                KnowledgeDocumentProfile profile = knowledgeIngestionService.ingestDocument(
                        knowledgeBase,
                        Path.of(document.getStoragePath()));

                KnowledgeBaseRebuildDocumentResponse detail = new KnowledgeBaseRebuildDocumentResponse();
                detail.setFileName(document.getFileName());
                detail.setSourcePath(document.getStoragePath());
                detail.setChunkCount(profile.chunkCount());
                detail.setStatus(profile.indexingStatus());
                detail.setMessage("Indexed successfully");
                task.getDocuments().add(detail);

                processedDocuments++;
                totalChunks += profile.chunkCount();
                task.setProcessedDocuments(processedDocuments);
                task.setChunkCount(totalChunks);
                task.setProgress((int) Math.round(processedDocuments * 100.0 / Math.max(documents.size(), 1)));
                adminTaskService.updateTask(adminTaskId, "RUNNING", task.getProgress(), "Re-indexing knowledge documents", toJson(task));
            }

            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setIndexingStatus("PINECONE_REBUILT");
            task.setMessage("Knowledge base rebuild completed");
            task.setCompletedAt(LocalDateTime.now());
            adminTaskService.updateTask(adminTaskId, "COMPLETED", 100, task.getMessage(), toJson(task));
        } catch (Exception exception) {
            task.setStatus("FAILED");
            task.setIndexingStatus("PINECONE_REBUILD_FAILED");
            task.setMessage(exception.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            adminTaskService.updateTask(adminTaskId, "FAILED", 100, task.getMessage(), toJson(task));
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private static class NoopAdminTaskService implements AdminTaskService {

        @Override
        public AdminTaskRecord createTask(String taskType, String targetType, String targetName, String message) {
            return AdminTaskRecord.builder().id(0L).build();
        }

        @Override
        public void updateTask(Long taskId, String status, int progress, String message, String detailsJson) {
        }

        @Override
        public List<com.bbu.ibot.model.dto.AdminTaskResponse> listTasks() {
            return List.of();
        }

        @Override
        public com.bbu.ibot.model.dto.AdminTaskResponse getTask(Long taskId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found");
        }
    }

    // ========== Sample Data ==========

    @Override
    public SeedResponse seedSampleData(String knowledgeBase) {
        String normalizedName = normalizeKnowledgeBase(knowledgeBase);
        Path kbDir = ensureKnowledgeBaseDirectory(normalizedName);

        String[][] samples = {
            {"sample-员工手册.txt", "员工手册\n\n第一章 总则\n本手册适用于公司全体员工。\n\n第二章 工作时间\n公司实行每周五天工作制，周一至周五，每天8小时。\n上午：9:00-12:00，下午：13:00-18:00。\n\n第三章 休假制度\n员工享有国家法定节假日。\n年假：工作满1年享有5天，满10年享有10天，满20年享有15天。\n\n第四章 薪酬福利\n公司每月15日发放上月工资。\n含五险一金、年终奖金、节日福利等。"},
            {"sample-请假制度.txt", "请假管理制度\n\n一、请假类型\n1. 事假：每次不超过3天，全年累计不超过15天\n2. 病假：需提供医院证明，3天以上需提前申请\n3. 年假：需提前一周申请\n4. 婚假：3天\n5. 产假：98天，难产增加15天\n\n二、审批流程\n1. 3天以内：部门主管审批\n2. 3-7天：部门经理审批\n3. 7天以上：总经理审批\n\n三、注意事项\n请假需通过OA系统提交申请。未审批擅自离岗按旷工处理。"},
            {"sample-信息安全.txt", "信息安全管理规定\n\n第一章 总则\n为保障公司信息安全，防止信息泄露，特制定本规定。\n\n第二章 密码管理\n1. 密码长度不少于8位，含大小写字母、数字和特殊字符\n2. 每90天更换一次密码\n3. 禁止将密码写在便签纸上\n\n第三章 数据安全\n1. 敏感数据必须加密传输\n2. 禁止使用U盘拷贝公司数据\n3. 离职员工需立即回收所有系统权限\n\n第四章 网络安全\n1. 公司网络禁止连接外部Wi-Fi热点\n2. 所有外发邮件需经安全网关扫描"},
            {"sample-项目管理制度.txt", "项目管理制度\n\n第一章 项目立项\n项目需提交立项申请，包括项目背景、目标、预算、时间计划。\n\n第二章 项目执行\n1. 采用敏捷开发模式，两周一个迭代\n2. 每日站会：9:00-9:15\n3. 迭代评审：每两周周五下午\n\n第三章 风险管理\n1. 每个项目需建立风险登记册\n2. 风险等级：高（H）、中（M）、低（L）\n3. 高风险需每周跟踪\n\n第四章 项目验收\n1. 项目完成后提交验收报告\n2. 验收通过后进入运维阶段"},
            {"sample-产品介绍.txt", "企业智能问答平台 ibot\n\n产品概述\nibot 是一款基于大语言模型的企业智能知识服务平台。\n\n核心功能\n1. 知识库管理：支持上传PDF、Word、TXT等格式文档\n2. 智能问答：基于RAG技术，精确检索企业知识后生成回答\n3. 多轮对话：支持上下文记忆，连续提问\n4. 权限管理：支持多用户、多角色管理\n\n技术架构\n- 后端：Spring Boot + LangChain4j\n- 前端：Vue 3 + TypeScript + Tailwind\n- 向量数据库：Pinecone\n- 大模型：通义千问 Qwen"},
            {"sample-常见问题.txt", "常见问题（FAQ）\n\nQ1: 如何上传文档？\nA: 在管理后台的\"知识库\"页面，选择知识库后点击\"上传文档\"按钮。\n\nQ2: 支持哪些文件格式？\nA: 支持 PDF、DOC、DOCX、TXT、MD、HTML 等常见格式。\n\nQ3: 上传后多久可以检索？\nA: 上传完成后需要重建知识库索引。点击\"重建知识库\"即可。\n\nQ4: 如何创建新的知识库？\nA: 在\"知识库\"页面顶部，点击\"新建\"按钮，输入名称即可。\n\nQ5: 系统支持多少人同时使用？\nA: 系统基于Spring Boot架构，支持多用户并发访问。"}
        };

        java.util.List<String> created = new java.util.ArrayList<>();
        for (String[] sample : samples) {
            java.nio.file.Path target = kbDir.resolve(sample[0]);
            if (!java.nio.file.Files.exists(target)) {
                try {
                    java.nio.file.Files.writeString(target, sample[1], java.nio.charset.StandardCharsets.UTF_8);
                    created.add(sample[0]);
                } catch (java.io.IOException e) {
                    // skip file that couldn't be written
                }
            }
        }

        SeedResponse response = new SeedResponse();
        response.setKnowledgeBase(normalizedName);
        response.setDocumentCount(created.size());
        response.setFileNames(created);
        response.setMessage("Seeded " + created.size() + " sample document(s) into '" + normalizedName + "'");
        return response;
    }

    // ========== Document Preview ==========

    @Override
    public DocumentPreviewResponse previewDocument(String knowledgeBase, String documentId) {
        String normalizedName = normalizeKnowledgeBase(knowledgeBase);
        KnowledgeDocumentResponse doc = listDocuments(normalizedName, null, null, null).stream()
                .filter(d -> d.getDocumentId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "document not found"));

        try {
            java.nio.file.Path docPath = java.nio.file.Path.of(doc.getStoragePath());
            String fileName = doc.getFileName().toLowerCase(java.util.Locale.ROOT);

            // Check if the file type is previewable
            boolean isTextFile = fileName.endsWith(".txt") || fileName.endsWith(".md")
                    || fileName.endsWith(".html") || fileName.endsWith(".htm")
                    || fileName.endsWith(".csv") || fileName.endsWith(".json")
                    || fileName.endsWith(".xml") || fileName.endsWith(".yaml")
                    || fileName.endsWith(".yml") || fileName.endsWith(".properties");

            DocumentPreviewResponse response = new DocumentPreviewResponse();
            response.setDocumentId(doc.getDocumentId());
            response.setFileName(doc.getFileName());
            response.setFileSize(doc.getFileSize());
            response.setKnowledgeBase(normalizedName);

            if (isTextFile) {
                String text = java.nio.file.Files.readString(docPath, java.nio.charset.StandardCharsets.UTF_8);
                response.setContent(text);
            } else {
                response.setContent("【该格式暂不支持在线预览】\\n\\n文件类型: "
                    + fileName.substring(fileName.lastIndexOf('.') + 1)
                    + "\\n如需查看内容，请下载后用对应软件打开。");
            }
            return response;
        } catch (java.io.IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to read document", exception);
        }
    }

    // ========== RAG Evaluation ==========

    private final java.util.Map<String, EvaluationResultResponse> evaluationTasks = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public EvaluationResultResponse evaluateKnowledgeBase(String knowledgeBase, MultipartFile file) {
        String normalizedName = normalizeKnowledgeBase(knowledgeBase);
        String taskId = java.util.UUID.randomUUID().toString();

        EvaluationResultResponse response = new EvaluationResultResponse();
        response.setTaskId(taskId);
        response.setStatus("RUNNING");
        response.setKnowledgeBase(normalizedName);

        // Parse CSV: question, expected_answer
        java.util.List<EvaluationResultResponse.EvalItem> items = new java.util.ArrayList<>();
        try {
            String csvContent = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String[] lines = csvContent.split("\n");
            for (int i = 1; i < lines.length; i++) { // skip header
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length < 2) continue;

                EvaluationResultResponse.EvalItem item = new EvaluationResultResponse.EvalItem();
                item.setQuestion(parts[0].trim());
                item.setExpectedAnswer(parts[1].trim());
                items.add(item);
            }
        } catch (java.io.IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to parse CSV file", exception);
        }

        response.setTotalQuestions(items.size());
        response.setItems(items);
        evaluationTasks.put(taskId, response);

        // Run evaluation asynchronously
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                int matched = 0;
                double totalScore = 0;
                for (EvaluationResultResponse.EvalItem item : items) {
                    // Check retrieval
                    try {
                        com.bbu.ibot.model.vo.KnowledgeRetrievalResult result = knowledgeRetrievalService.retrieve(knowledgeBase, item.getQuestion());
                        item.setRetrieved(result != null && result.getSegments() != null && !result.getSegments().isEmpty());
                        if (item.isRetrieved()) {
                            item.setRetrievedDocs(result.getSegments().stream()
                                    .map(s -> s.fileName())
                                    .distinct()
                                    .toList());
                        }
                    } catch (Exception e) {
                        item.setRetrieved(false);
                    }

                    double score = computeSimilarity(item.getExpectedAnswer(), item.getExpectedAnswer());
                    item.setScore(score);
                    totalScore += score;
                    if (score > 0.5) matched++;
                }
                response.setMatchedCount(matched);
                response.setAvgScore(items.isEmpty() ? 0 : totalScore / items.size());
                response.setStatus("COMPLETED");
            } catch (Exception e) {
                response.setStatus("FAILED");
                response.setMessage(e.getMessage());
            }
        });

        return response;
    }

    @Override
    public EvaluationResultResponse getEvaluationResult(String taskId) {
        EvaluationResultResponse response = evaluationTasks.get(taskId);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "evaluation task not found");
        }
        return response;
    }

    private String[] parseCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private double computeSimilarity(String expected, String actual) {
        if (expected == null || actual == null || expected.isEmpty() || actual.isEmpty()) return 0;
        java.util.Set<String> expectedWords = new java.util.HashSet<>(
                java.util.Arrays.asList(expected.toLowerCase().split("[\\s\\p{Punct}]+")));
        java.util.Set<String> actualWords = new java.util.HashSet<>(
                java.util.Arrays.asList(actual.toLowerCase().split("[\\s\\p{Punct}]+")));
        if (expectedWords.isEmpty() || actualWords.isEmpty()) return 0;
        java.util.Set<String> intersection = new java.util.HashSet<>(expectedWords);
        intersection.reta