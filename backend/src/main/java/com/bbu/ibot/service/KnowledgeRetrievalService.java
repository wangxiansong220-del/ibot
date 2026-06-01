package com.bbu.ibot.service;

import com.bbu.ibot.model.vo.KnowledgeRetrievalResult;

public interface KnowledgeRetrievalService {

    KnowledgeRetrievalResult retrieve(String knowledgeBase, String question);
}
