package com.bbu.ibot.model.vo;

public record RetrievedSegment(String content, double score, String sourcePath, String fileName, String chunkIndex) {
}
