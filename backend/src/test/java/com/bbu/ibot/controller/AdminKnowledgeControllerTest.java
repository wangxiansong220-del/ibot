package com.bbu.ibot.controller;

import com.bbu.ibot.model.dto.AdminDocumentUploadResponse;
import com.bbu.ibot.model.dto.KnowledgeBaseRebuildResponse;
import com.bbu.ibot.model.dto.KnowledgeDocumentResponse;
import com.bbu.ibot.model.dto.RagStatusResponse;
import com.bbu.ibot.service.KnowledgeBaseAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminKnowledgeControllerTest {

    @Mock
    private KnowledgeBaseAdminService knowledgeBaseAdminService;

    @InjectMocks
    private AdminKnowledgeController adminKnowledgeController;

    @Test
    void shouldUploadKnowledgeDocuments() {
        KnowledgeDocumentResponse document = new KnowledgeDocumentResponse();
        document.setDocumentId("doc-1");
        document.setKnowledgeBase("enterprise");
        document.setFileName("handbook.txt");
        document.setIndexingStatus("PINECONE_DISABLED");
        document.setUploadedAt(LocalDateTime.of(2026, 3, 30, 17, 0));

        AdminDocumentUploadResponse response = new AdminDocumentUploadResponse();
        response.setKnowledgeBase("enterprise");
        response.setUploadedCount(1);
        response.setDocuments(List.of(document));

        MockMultipartFile file = new MockMultipartFile("files", "handbook.txt", "text/plain", "hello".getBytes());
        when(knowledgeBaseAdminService.uploadDocuments("enterprise", new MockMultipartFile[]{file})).thenReturn(response);

        AdminDocumentUploadResponse actual = adminKnowledgeController.uploadDocuments("enterprise", new MockMultipartFile[]{file});

        assertThat(actual.getKnowledgeBase()).isEqualTo("enterprise");
        assertThat(actual.getUploadedCount()).isEqualTo(1);
    }

    @Test
    void shouldListKnowledgeDocuments() {
        KnowledgeDocumentResponse document = new KnowledgeDocumentResponse();
        document.setDocumentId("doc-1");
        document.setKnowledgeBase("enterprise");
        document.setFileName("faq.pdf");

        when(knowledgeBaseAdminService.listDocuments("enterprise", null, null, null)).thenReturn(List.of(document));

        List<KnowledgeDocumentResponse> actual = adminKnowledgeController.listDocuments("enterprise", null, null, null);

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getFileName()).isEqualTo("faq.pdf");
    }

    @Test
    void shouldDeleteKnowledgeDocument() {
        when(knowledgeBaseAdminService.deleteDocument("enterprise", "doc-1")).thenReturn("document deleted");

        var response = adminKnowledgeController.deleteDocument("enterprise", "doc-1");

        verify(knowledgeBaseAdminService).deleteDocument("enterprise", "doc-1");
        assertThat(response.message()).isEqualTo("document deleted");
    }

    @Test
    void shouldTriggerKnowledgeBaseRebuild() {
        KnowledgeBaseRebuildResponse response = new KnowledgeBaseRebuildResponse();
        response.setTaskId("task-1");
        response.setKnowledgeBase("enterprise");
        response.setDocumentCount(2);

        when(knowledgeBaseAdminService.rebuildKnowledgeBase("enterprise")).thenReturn(response);

        KnowledgeBaseRebuildResponse actual = adminKnowledgeController.rebuildKnowledgeBase("enterprise");

        assertThat(actual.getTaskId()).isEqualTo("task-1");
        assertThat(actual.getDocumentCount()).isEqualTo(2);
    }

    @Test
    void shouldReturnRebuildTaskDetails() {
        KnowledgeBaseRebuildResponse response = new KnowledgeBaseRebuildResponse();
        response.setTaskId("task-1");
        response.setProgress(50);
        response.setStatus("RUNNING");

        when(knowledgeBaseAdminService.getRebuildTask("task-1")).thenReturn(response);

        KnowledgeBaseRebuildResponse actual = adminKnowledgeController.getRebuildTask("task-1");

        assertThat(actual.getTaskId()).isEqualTo("task-1");
        assertThat(actual.getProgress()).isEqualTo(50);
    }

    @Test
    void shouldReturnRagStatus() {
        RagStatusResponse response = new RagStatusResponse();
        response.setKnowledgeBase("enterprise");
        response.setDocumentCount(3);
        response.setRetrievalStatus("PINECONE_DISABLED");

        when(knowledgeBaseAdminService.getRagStatus("enterprise")).thenReturn(response);

        RagStatusResponse actual = adminKnowledgeController.getRagStatus("enterprise");

        assertThat(actual.getKnowledgeBase()).isEqualTo("enterprise");
        assertThat(actual.getRetrievalStatus()).isEqualTo("PINECONE_DISABLED");
    }
}
