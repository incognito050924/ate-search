package com.richslide.atesearch.business.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richslide.atesearch.business.helper.CrawlingException;

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

import java.util.Map;

public abstract class DefaultService<T> {
    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    protected Client client;

    public IndexResponse indexes(final String indexName, final String typeName, final String id, final Map<String, Object> json) {
        return indexRequestBuilder(indexName, typeName, id)
                .setSource(json)
                .get();
    }

    public IndexResponse indexes(final String indexName, final String typeName, final Map<String, Object> json) {
        return indexRequestBuilder(indexName, typeName)
                .setSource(json)
                .get();
    }

    public IndexResponse indexes(final String indexName, final String typeName, final String id, T bean) throws CrawlingException {
        try {
            String json = new ObjectMapper().writeValueAsString(bean);
            return indexRequestBuilder(indexName, typeName, id)
                    .setSource(json, XContentType.JSON)
                    .get();

        } catch (JsonProcessingException e) {
            throw new CrawlingException(e.getMessage(), e);
        }
    }

    public IndexResponse indexes(final String indexName, final String typeName, T bean) throws CrawlingException {
        try {
            String json = new ObjectMapper().writeValueAsString(bean);
            return indexRequestBuilder(indexName, typeName)
                    .setSource(json, XContentType.JSON)
                    .get();

        } catch (JsonProcessingException e) {
            throw new CrawlingException(e.getMessage(), e);
        }
    }

    private IndexRequestBuilder indexRequestBuilder(final String indexName, final String typeName) {
        return client.prepareIndex(indexName, typeName);
    }

    private IndexRequestBuilder indexRequestBuilder(final String indexName, final String typeName, final String id) {
        return client.prepareIndex(indexName, typeName, id);
    }

}
