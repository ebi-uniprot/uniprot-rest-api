package uk.ac.ebi.uniprot.uuw.suggester.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Solr properties bean that will be injected with values from application.properties.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
@ConfigurationProperties(prefix = "solr")
public class SolrConfigProperties {
    private String url;
    private boolean useCloudClient;
    private int queueSize;
    private int threadCount;
    private String collectionName;
    private String idFieldName;


    public boolean isUseCloudClient() {
        return useCloudClient;
    }

    public void setUseCloudClient(boolean useCloudClient) {
        this.useCloudClient = useCloudClient;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getIdFieldName() {
        return idFieldName;
    }

    public void setIdFieldName(String idFieldName) {
        this.idFieldName = idFieldName;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
