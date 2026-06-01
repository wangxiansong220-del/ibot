package com.bbu.ibot.service.impl;

import com.bbu.ibot.model.vo.KnowledgeBaseIngestionSummary;
import com.bbu.ibot.model.vo.KnowledgeDocumentProfile;
import com.bbu.ibot.service.KnowledgeIngestionService;
import com.bbu.ibot.store.PineconeEmbeddingStoreFactory;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PineconeKnowledgeIngestionServiceImpl implements KnowledgeIngestionService {

    private static final String STATUS_READY = "PINECONE_READY_FOR_INGESTION";
    private static final String STATUS_INGESTED = "PINECONE_INGESTED";
    private static final String STATUS_REBUILT = "PINECONE_REBUILT";

    private final EmbeddingModel embeddingModel;
    private final PineconeEmbeddingStoreFactory pineconeEmbeddingStoreFactory;
    private final DocumentSplitter documentSplitter;

    public PineconeKnowledgeIngestionServiceImpl(
            @Qualifier("ibotEmbeddingModel") EmbeddingModel embeddingModel,
            PineconeEmbeddingStoreFactory pineconeEmbeddingStoreFactory) {
        this.embeddingModel = embeddingModel;
        this.pineconeEmbeddingStoreFactory = pineconeEmbeddingStoreFactory;
        this.documentSplitter = DocumentSplitters.recursive(800, 120);
    }

    @Override
    public KnowledgeDocumentProfile inspectDocument(String knowledgeBase, Path file) {
        String namespace = normalizeNamespace(knowledgeBase);
        List<TextSegment> segments = splitIntoSegments(knowledgeBase, file);
        return new KnowledgeDocumentProfile(namespace, segments.size(), STATUS_READY);
    }

    @Override
    public KnowledgeDocumentProfile ingestDocument(String knowledgeBase, Path file) {
        String namespace = normalizeNamespace(knowledgeBase);
        String documentId = buildDocumentId(file);
        List<TextSegment> sanitizedSegments = prepareSegments(knowledgeBase, namespace, documentId, file);
        List<Embedding> embeddings = embedSegments(sanitizedSegments);
        EmbeddingStore<TextSegment> embeddingStore = pineconeEmbeddingStoreFactory.create(namespace);
        embeddingStore.addAll(generateIds(documentId, sanitizedSegments.size()), embeddings, sanitizedSegments);
        return new KnowledgeDocumentProfile(namespace, sanitizedSegments.size(), STATUS_INGESTED);
    }

    @Override
    public void clearKnowledgeBase(String knowledgeBase) {
        String namespace = normalizeNamespace(knowledgeBase);
        pineconeEmbeddingStoreFactory.create(namespace).removeAll();
    }

    @Override
    public KnowledgeBaseIngestionSummary rebuildKnowledgeBase(String knowledgeBase, List<Path> files) {
        String namespace = normalizeNamespace(knowledgeBase);
        clearKnowledgeBase(knowledgeBase);

        int totalChunks = 0;
        for (Path file : files) {
            totalChunks += ingestDocument(knowledgeBase, file).chunkCount();
        }

        return new KnowledgeBaseIngestionSummary(namespace, totalChunks, STATUS_REBUILT);
    }

    private List<TextSegment> splitIntoSegments(String knowledgeBase, Path file) {
        validateSupportedFile(file);
        Document document = loadDocument(knowledgeBase, file);
        return documentSplitter.split(document);
    }

    private List<TextSegment> prepareSegments(String knowledgeBase, String namespace, String documentId, Path file) {
        List<TextSegment> rawSegments = splitIntoSegments(knowledgeBase, file);
        List<TextSegment> sanitizedSegments = new ArrayList<>();
        for (int i = 0; i < rawSegments.size(); i++) {
            TextSegment segment = rawSegments.get(i);
            Metadata metadata = new Metadata()
                    .put("knowledge_base", knowledgeBase)
                    .put("namespace", namespace)
                    .put("document_id", documentId)
                    .put("file_name", file.getFileName().toString())
                    .put("source_path", file.toAbsolutePath().toString())
                    .put("chunk_index", String.valueOf(i));

            for (Map.Entry<String, Object> entry : segment.metadata().toMap().entrySet()) {
                metadata.put(entry.getKey(), String.valueOf(entry.getValue()));
            }

            sanitizedSegments.add(TextSegment.from(segment.text(), metadata));
        }
        return sanitizedSegments;
    }

    private Document loadDocument(String knowledgeBase, Path file) {
        try {
            String extension = extensionOf(file);
            String normalizedText = switch (extension) {
                case "html", "htm" -> normalizeHtml(Files.readString(file, StandardCharsets.UTF_8));
                case "md", "txt" -> Files.readString(file, StandardCharsets.UTF_8);
                case "pdf" -> extractPdfText(file);
                case "doc" -> extractDocText(file);
                case "docx" -> extractDocxText(file);
                case "pptx" -> extractPptxText(file);
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "unsupported file type: " + extension);
            };

            Metadata metadata = new Metadata()
                    .put(Document.FILE_NAME, file.getFileName().toString())
                    .put("knowledge_base", knowledgeBase)
                    .put("source_path", file.toAbsolutePath().toString());

            return Document.from(normalizedText, metadata);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to read knowledge-base file", exception);
        }
    }

    private List<Embedding> embedSegments(List<TextSegment> segments) {
        try {
            return embeddingModel.embedAll(segments).content();
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "failed to generate embeddings", exception);
        }
    }

    private List<String> generateIds(String documentId, int chunkCount) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            ids.add(documentId + "-chunk-" + i);
        }
        return ids;
    }

    private void validateSupportedFile(Path file) {
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "knowledge-base file not found");
        }
        String extension = extensionOf(file);
        if (!List.of("txt", "md", "html", "htm", "pdf", "doc", "docx", "pptx").contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported file type: " + extension);
        }
    }

    private String extensionOf(Path file) {
        String fileName = file.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeNamespace(String knowledgeBase) {
        if (!StringUtils.hasText(knowledgeBase)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "knowledgeBase cannot be blank");
        }
        return knowledgeBase.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-_]", "-");
    }

    private String buildDocumentId(Path file) {
        return UUID.nameUUIDFromBytes(file.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String normalizeHtml(String content) {
        return content
                .replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?s)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractPdfText(Path file) {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to parse pdf file", exception);
        }
    }

    private String extractDocxText(Path file) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(Files.readAllBytes(file)))) {
            StringBuilder text = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> {
                if (StringUtils.hasText(paragraph.getText())) {
                    if (!text.isEmpty()) {
                        text.append('\n');
                    }
                    text.append(paragraph.getText());
                }
            });
            return text.toString().replaceAll("\\s+", " ").trim();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to parse docx file", exception);
        }
    }

    private String extractDocText(Path file) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to read doc file", exception);
        }

        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            return Arrays.stream(extractor.getParagraphText())
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("\n"))
                    .replaceAll("\\s+", " ")
                    .trim();
        } catch (IOException | RuntimeException exception) {
            String fallbackText = extractPrintableText(bytes);
            if (StringUtils.hasText(fallbackText)) {
                return fallbackText;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to parse doc file", exception);
        }
    }

    private String extractPptxText(Path file) {
        try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(Files.readAllBytes(file)))) {
            StringBuilder text = new StringBuilder();
            for (XSLFSlide slide : slideShow.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape && StringUtils.hasText(textShape.getText())) {
                        if (!text.isEmpty()) {
                            text.append('\n');
                        }
                        text.append(textShape.getText());
                    }
                }
            }
            return text.toString().replaceAll("\\s+", " ").trim();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to parse pptx file", exception);
        }
    }

    private String extractPrintableText(byte[] bytes) {
        List<String> asciiCandidates = extractAsciiStrings(bytes);
        List<String> utf16Candidates = extractUtf16LeStrings(bytes);

        return java.util.stream.Stream.concat(asciiCandidates.stream(), utf16Candidates.stream())
                .map(value -> value.replaceAll("\\s+", " ").trim())
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private List<String> extractAsciiStrings(byte[] bytes) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (byte value : bytes) {
            int unsigned = value & 0xFF;
            if (unsigned >= 32 && unsigned <= 126) {
                current.append((char) unsigned);
            } else {
                flushStringCandidate(values, current, 6);
            }
        }
        flushStringCandidate(values, current, 6);
        return values;
    }

    private List<String> extractUtf16LeStrings(byte[] bytes) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i + 1 < bytes.length; i += 2) {
            int low = bytes[i] & 0xFF;
            int high = bytes[i + 1] & 0xFF;
            if (high == 0 && low >= 32 && low <= 126) {
                current.append((char) low);
            } else {
                flushStringCandidate(values, current, 4);
            }
        }
        flushStringCandidate(values, current, 4);
        return values;
    }

    private void flushStringCandidate(List<String> values, StringBuilder current, int minLength) {
        if (current.length() >= minLength) {
            values.add(current.toString());
        }
        current.setLength(0);
    }
}
