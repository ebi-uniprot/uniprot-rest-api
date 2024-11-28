import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.async.download.model.request.mapto.UniRefToUniProtKBDownloadRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.store.search.SolrCollection;

import static org.junit.jupiter.api.Assertions.*;

class UniRefToUniProtKBDownloadRequestTest {

    private UniRefToUniProtKBDownloadRequest request;

    @BeforeEach
    void setUp() {
        request = new UniRefToUniProtKBDownloadRequest();
    }

    @Test
    void testInitialFieldValues() {
        // Default values should be null or false
        assertNull(request.getFormat());
        assertNull(request.getFields());
        assertNull(request.getDownloadJobId());
        assertFalse(request.isForce());
    }

    @Test
    void testSetAndGetFormat() {
        // Set the format
        String format = "json";
        request.setFormat(format);

        // Verify that the format is parsed using UniProtKBRequestUtil
        String expectedFormat = UniProtKBRequestUtil.parseFormat(format);
        assertEquals(expectedFormat, request.getFormat());
    }

    @Test
    void testSetAndGetFields() {
        // Set and get fields
        String fields = "gene_name,protein_name";
        request.setFields(fields);
        assertEquals(fields, request.getFields());
    }

    @Test
    void testSetAndGetDownloadJobId() {
        // Set and get downloadJobId
        String downloadJobId = "12345";
        request.setDownloadJobId(downloadJobId);
        assertEquals(downloadJobId, request.getDownloadJobId());
    }

    @Test
    void testSetAndIsForce() {
        // Set and check the force flag
        request.setForce(true);
        assertTrue(request.isForce());
    }

    @Test
    void testGetToMethod() {
        // Verify that getTo returns the name of the uniprot SolrCollection
        assertEquals(SolrCollection.uniprot.name(), request.getTo());
    }

    @Test
    void testGetFromMethod() {
        // Verify that getFrom returns the name of the uniref SolrCollection
        assertEquals(SolrCollection.uniref.name(), request.getFrom());
    }
}
