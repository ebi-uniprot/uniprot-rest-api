package uk.ac.ebi.uniprot.api.keyword;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Created 28/06/19
 *
 * @author Edd
 */
@RunWith(SpringRunner.class)
@WebMvcTest(KeywordController.class)
@AutoConfigureRestDocs
@TestPropertySource(properties = "spring.jackson.serialization.indent_output=true")
public class KeywordControllerAPIDocumentationTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuthorRepository repository;
}
