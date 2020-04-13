package org.uniprot.api.repository;

import java.io.File;
import java.nio.file.Files;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.DisposableBean;

@Slf4j
public class SolrTestConfig implements DisposableBean {
    private static final String TEMP_DIR_PREFIX = "test-solr-data-dir";
    private final File file;

    private SolrTestConfig() throws Exception {
        file = Files.createTempDirectory(TEMP_DIR_PREFIX).toFile();
    }

    @Override
    public void destroy() throws Exception {
        if (file != null) {
            FileUtils.deleteDirectory(file);
            log.info("Deleted solr home");
        }
    }
}
