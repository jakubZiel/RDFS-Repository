package com.rdfsonto.infrastructure.config.elastic;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.TransportUtils;
import co.elastic.clients.transport.rest_client.RestClientTransport;


@Configuration
public class ElasticSearchConfig
{
    @Value("${elastic.host}")
    private String host;
    @Value("${elastic.port}")
    private int port;
    @Value("${elastic.login}")
    private String login;
    @Value("${elastic.password}")
    private String password;
    @Value("${elastic.fingerprint}")
    private String fingerprint;

    @Bean
    RestClient restClient()
    {
        final var sslContext = TransportUtils.sslContextFromCaFingerprint(fingerprint);

        final var credentialProvider = new BasicCredentialsProvider();
        credentialProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(login, password));

        return RestClient.builder(new HttpHost(host, port, "http"))
/*            .setHttpClientConfigCallback(hc -> hc
                .setSSLContext(sslContext)
                .setDefaultCredentialsProvider(credentialProvider))*/
            .build();
    }

    @Bean
    ElasticsearchClient elasticsearchClient(final RestClient restClient)
    {
        final var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    @Bean
    ElasticsearchAsyncClient elasticsearchAsyncClient(final RestClient restClient)
    {
        final var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchAsyncClient(transport);
    }
}
