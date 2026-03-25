package com.aigateway.core.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Value("${gateway.webclient.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${gateway.webclient.read-timeout-ms:120000}")
    private int readTimeoutMs;

    @Value("${gateway.webclient.max-in-memory-size-mb:32}")
    private int maxInMemorySizeMb;

    @Bean
    public WebClient.Builder webClientBuilder() {
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(readTimeoutMs))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS)));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(config -> config.defaultCodecs()
                .maxInMemorySize(maxInMemorySizeMb * 1024 * 1024))
            .build();

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies);
    }
}
