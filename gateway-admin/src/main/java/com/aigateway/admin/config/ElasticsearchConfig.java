package com.aigateway.admin.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 手动配置 ElasticsearchClient，注册 JavaTimeModule 以支持 LocalDateTime 序列化
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUri;

    @Bean
    @ConditionalOnMissingBean
    public RestClient restClient() {
        String uri = esUri.replace("http://", "").replace("https://", "");
        String[] parts = uri.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
        return RestClient.builder(new HttpHost(host, port, "http")).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(mapper);
        RestClientTransport transport = new RestClientTransport(restClient, jsonpMapper);
        return new ElasticsearchClient(transport);
    }
}
