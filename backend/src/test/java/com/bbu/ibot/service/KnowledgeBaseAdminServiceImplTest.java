package com.bbu.ibot.service;

import com.bbu.ibot.model.dto.AdminDocumentUploadResponse;
import com.bbu.ibot.model.dto.KnowledgeBaseRebuildResponse;
import com.bbu.ibot.model.dto.KnowledgeDocumentResponse;
import com.bbu.ibot.model.dto.RagStatusResponse;
import com.bbu.ibot.model.vo.KnowledgeBaseIngestionSummary;
import com.bbu.ibot.model.vo.KnowledgeDocumentProfile;
import com.bbu.ibot.service.impl.KnowledgeBaseAdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeBaseAdminServiceImplTest {

    @TempDir
    Path tempDir;

    private KnowledgeIngestionService mockKnowledgeIngestionService() {
        KnowledgeIngestionService service = mock(KnowledgeIngestionService.class);
        when(service.inspectDocument(eq("enterprise"), any(Path.class)))
                .thenReturn(new KnowledgeDocumentProfile("enterprise", 1, "PINECONE_READY_FOR_INGESTION"));
        when(service.ingestDocument(eq("enterprise"), any(Path.class)))
                .thenReturn(new KnowledgeDocumentProfile("enterprise", 1, "PINECONE_INGESTED"));
        when(service.rebuildKnowledgeBase(eq("enterprise"), any()))
                .thenReturn(new KnowledgeBaseIngestionSummary("enterprise", 2, "PINECONE_REBUILT"));
        return service;
    }

    @Test
    void shouldUploadAndListDocuments() {
        KnowledgeBaseAdminService service = new KnowledgeBaseAdminServiceImpl(
                tempDir.toString(), false, "", "", "ibot-default", "dashscope-embedding", 800, 120,
                mockKnowledgeIngestionService());

        MockMultipartFile file = new MockMultipartFile(
                "files", "manual.txt", "text/plain", "internal handbook".getBytes());

        AdminDocumentUploadResponse uploadResponse = service.uploadDocuments("enterprise", new MockMultipartFile[]{file});
        List<KnowledgeDocumentResponse> documents = service.listDocuments("enterprise", null, null, null);

        assertEquals(1, uploadResponse.getUploadedCount());
        assertEquals("Uploaded 1 document(s)", uploadResponse.getMessage());
        assertEquals(1, documents.size());
        assertEquals("enterprise", documents.get(0).getKnowledgeBase());
        assertEquals("PINECONE_READY_FOR_INGESTION", documents.get(0).getIndexingStatus());
    }

    @Test
    void shouldReturnRagSkeletonStatus() {
        KnowledgeBaseAdminService service = new KnowledgeBaseAdminServiceImpl(
                tempDir.toString(), false, "", "", "ibot-default", "dashscope-embedding", 800, 120,
                mockKnowledgeIngestionService());

        RagStatusResponse response = service.getRagStatus("enterprise");

        assertEquals("enterprise", response.getKnowledgeBase());
        assertEquals("pinecone", response.getVectorStore());
        assertFalse(response.isRagEnabled());
        assertEquals("PINECONE_DISABLED", response.getRetrievalStatus());
    }

    @Test
    void shouldBuildKnowledgeBaseSummaryFromStoredDocuments() {
        KnowledgeBaseAdminService service = new KnowledgeBaseAdminServiceImpl(
                tempDir.toString(), true, "test-key", "ibot-index", "ibot-default", "dashscope-embedding", 800, 120,
                mockKnowledgeIngestionService());

        MockMultipartFile fileA = new MockMultipartFile("files", "a.txt", "text/plain", "A".getBytes());
        MockMultipartFile fileB = new MockMultipartFile("files", "b.txt", "text/plain", "B".getBytes());
        service.uploadDocuments("enterprise", new MockMultipartFile[]{fileA, fileB});

        KnowledgeBaseRebuildResponse response = service.rebuildKnowledgeBase("enterprise");

        assertNotNull(response.getTaskId());
        assertEquals(2, response.getDocumentCount());

        KnowledgeBaseRebuildResponse completed = waitForTaskCompletion(service, response.getTaskId());
        assertEquals("COMPLETED", completed.getStatus());
        assertEquals("PINECONE_REBUILT", completed.getIndexingStatus());
        assertEquals(2, completed.getChunkCount());
        assertEquals(2, completed.getProcessedDocuments());
    }

    @Test
    void shouldRejectBlankKnowledgeBase() {
        KnowledgeBaseAdminService service = new KnowledgeBaseAdminServiceImpl(
                tempDir.toString(), false, "", "", "ibot-default", "dashscope-embedding", 800, 120,
                mockKnowledgeIngestionService());

        assertThrows(ResponseStatusException.class, () -> service.listDocuments(" ", null, null, null));
    }

    @Test
    void shouldKeepDocumentWhenPineconeSyncFailsDuringUpload() {
        KnowledgeIngestionService ingestionService = mock(KnowledgeIngestionService.class);
        when(ingestionService.ingestDocument(eq("enterprise"), any(Path.class)))
                .thenThrow(new RuntimeException("tls negotiation failed"));
        when(ingestionService.inspectDocument(eq("enterprise"), any(Path.class)))
                .thenReturn(new KnowledgeDocumentProfile("enterprise", 2, "PINECONE_READY_FOR_INGESTION"));

        KnowledgeBaseAdminService service = new KnowledgeBaseAdminServiceImpl(
                tempDir.toString(), true, "test-key", "ibot-index", "ibot-default", "dashscope-embedding", 800, 120,
                ingestionService);

        MockMultipartFile file = new MockMultipartFile("files", "manual.txt", "text/plain", "internal handbook".getBytes());

        AdminDocumentUploadResponse response = service.uploadDocuments("enterprise", new MockMultipartFile[]{file});

        assertEquals(1, response.getUploadedCount());
        assertTrue(response.getMessage().contains("with warnings"));
        assertEquals("PINECONE_SYNC_FAILED", response.getDocuments().get(0).getIndexingStatus());
    }

    @Test
    void shouldDeleteLocalDocumentEvenWhenPineconeSyncFails() {
        KnowledgeIngestionService ingestionService = mock(KnowledgeIngestionService.class);
        when(ingestionService.ingestDocument(eq("enterprise"), any(Path.class)))
                .thenReturn(new KnowledgeDocumentProfile("enterprise", 1, "PINECONE_INGESTED"));
        when(ingestionService.inspectDocument(eq("enterprise"), any(Path.class)))
                .thenReturn(new KnowledgeDocumentProfile("enterprise", 1, "PINECONE_READY_FOR_INGESTION"));
        doThrow(new RuntimeException("pinecone unavailable")).when(ingestionService).clearKnowledgeBase("enterprise");

        KnowledgeBaseAdminService service = new KnowledgeBaseAdminServiceImpl(
                tempDir.toString(), true, "test-key", "ibot-index", "ibot-default", "dashscope-embedding", 800, 120,
                ingestionService);

        MockMultipartFile file = new MockMultipartFile("files", "manual.txt", "text/plain", "internal handbook".getBytes());
        AdminDocumentUploadResponse uploadResponse = service.uploadDocuments("enterprise", new MockMultipartFile[]{file});

        String message = service.deleteDocument("enterprise", uploadResponse.getDocuments().get(0).getDocumentId());

        assertTrue(message.contains("document deleted locally"));
        assertTrue(service.listDocuments("enterprise", null, null, null).isEmpty());
    }

    private KnowledgeBaseRebuildResponse waitForTaskCompletion(KnowledgeBaseAdminService service, String taskId) {
        long deadline = System.currentTimeMillis() + 3_000;
        while (System.currentTimeMillis() < deadline) {
            KnowledgeBaseRebuildResponse response = service.getRebuildTask(taskId);
            if ("COMPLETED".equals(response.getStatus()) || "FAILED".equals(response.getStatus())) {
                return response;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting for rebuild task to complete");
            }
        }

        fail("Timed out waiting for rebuild task to complete");
        return null;
    }
}
