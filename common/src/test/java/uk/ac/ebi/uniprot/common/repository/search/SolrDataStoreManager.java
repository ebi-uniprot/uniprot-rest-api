package uk.ac.ebi.uniprot.common.repository.search;

import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Represents a Solr data directory manager. Since this is to be used in tests only, it is responsible for
 * creating a temporary folder, which will be used to store Solr data. This class contains a {@link #cleanUp()} method
 * triggered by Spring, which will delete the temporary directory when the application context is destroyed.
 *
 * Created 18/09/18
 *
 * @author Edd
 */
public class SolrDataStoreManager {
    private static final String SOLR_SYSTEM_PROPERTIES = "solr-system.properties";
    private TemporaryFolder temporaryFolder = new TemporaryFolder();

    public SolrDataStoreManager() throws IOException {
        temporaryFolder.create();
        System.setProperty("solr.data.dir", temporaryFolder.getRoot().getAbsolutePath());
        loadPropertiesAndSetAsSystemProperties();
    }

    public void cleanUp() {
        temporaryFolder.delete();
    }

    private static void loadPropertiesAndSetAsSystemProperties() throws IOException {
        Properties properties = new Properties();
        InputStream propertiesStream = SolrDataStoreManager.class.getClassLoader()
                .getResourceAsStream(SOLR_SYSTEM_PROPERTIES);
        properties.load(propertiesStream);

        for (String property : properties.stringPropertyNames()) {
            System.setProperty(property, properties.getProperty(property));
        }
    }
}
