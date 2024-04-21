package edu.neu.InsurancePlan.config;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.http.HttpHeaders;

@Configuration
public class ElasticSearchConfig extends AbstractElasticsearchConfiguration {

    /**
     * When security is enabled with AbstractElasticsearchConfiguration extended
     */
    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {

        final ClientConfiguration clientConfiguration = ClientConfiguration
                .builder()
                .connectedTo("localhost:9200")
                .withBasicAuth("elastic", "CqSDwHSRn71dV5d8M4bx")
                .build();

        return RestClients.create(clientConfiguration).rest();
    }

    /*
     * @Bean
     * public RestHighLevelClient ecClient() {
     * final CredentialsProvider credentialsProvider = new
     * BasicCredentialsProvider();
     * credentialsProvider.setCredentials(AuthScope.ANY,
     * new UsernamePasswordCredentials("elastic", "_vrwoV04guoNrQJ=8BiL"));
     * 
     * RestClientBuilder builder = RestClient.builder(new HttpHost("localhost",
     * 9200, "http"))
     * .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback()
     * {
     * 
     * @Override
     * public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder
     * httpClientBuilder) {
     * return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
     * }
     * });
     * 
     * return new RestHighLevelClient(builder);
     * }
     */

    /**
     * When security is disabled create just the bean
     */
    /*
     * @Bean(destroyMethod = "close")
     * public RestHighLevelClient elasticsearchClient() {
     * final CredentialsProvider credentialsProvider = new
     * BasicCredentialsProvider();
     * credentialsProvider.setCredentials(AuthScope.ANY,
     * new UsernamePasswordCredentials("elastic", "_vrwoV04guoNrQJ=8BiL"));
     * 
     * RestClientBuilder builder = RestClient.builder(new HttpHost("localhost",
     * 9200, "https"))
     * .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback()
     * {
     * 
     * @Override
     * public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder
     * httpClientBuilder) {
     * return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
     * }
     * })
     * .setDefaultHeaders(compatibilityHeaders());
     * 
     * return new RestHighLevelClient(builder);
     * }
     * 
     * private Header[] compatibilityHeaders() {
     * return new Header[]{new BasicHeader(HttpHeaders.ACCEPT,
     * "application/vnd.elasticsearch+json;compatible-with=7"), new
     * BasicHeader(HttpHeaders.CONTENT_TYPE,
     * "application/vnd.elasticsearch+json;compatible-with=7")};
     * }
     */

}
