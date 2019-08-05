package org.uniprot.api.uniprotkb.output.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.uniprotkb.output.converter.UniProtKBJsonMessageConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Created 08/11/18
 *
 * @author Edd
 */
public class UniProtKBJsonMessageConverterTest {
    private UniProtKBJsonMessageConverterTester converter;
    private ByteArrayOutputStream outputStream;

    @Before
    public void setUp() {
        converter = new UniProtKBJsonMessageConverterTester();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void hasCorrectMediaType() {
        assertThat(converter.getSupportedMediaTypes(), is(singletonList(APPLICATION_JSON)));
    }

    @Test
    public void beforeWritesCorrectlyWithoutFacets() throws IOException {
        // only interested in response being a valid JSON object (not actual entities written)
        MessageConverterContext<String> context = MessageConverterContext.<String>builder()
                .build();
        converter.before(context, outputStream);
        converter.after(context, outputStream);

        ObjectMapper om = new ObjectMapper();
        ResponseType response = om.readValue(outputStream.toString(), ResponseType.class);

        assertThat(response.getResults(), is(emptyList()));
        assertThat(response.getFacets(), is(nullValue()));
    }

    @Test
    public void beforeWritesCorrectlyWithFacets() throws IOException {
        // only interested in response being a valid JSON object (not actual entities written)

        // given
        String label1 = "label1";
        String id1 = "id1";
        boolean multipleSelection = false;
        String facet1Item1Label = "id1item1Label";
        String facet1Item1Value = "id1item1Value";
        String facet1Item2Label = "id1item2Label";
        String facet1Item2Value = "id1item2Value";
        long facet1Item1Count = 1L;
        long facet1Item2Count = 2L;
        Facet facet1 = Facet
                .builder()
                .label(label1)
                .name(id1)
                .allowMultipleSelection(multipleSelection)
                .values(asList(FacetItem.builder()
                                       .count(facet1Item1Count)
                                       .label(facet1Item1Label)
                                       .value(facet1Item1Value)
                                       .build(),
                               FacetItem.builder()
                                       .count(facet1Item2Count)
                                       .label(facet1Item2Label)
                                       .value(facet1Item2Value)
                                       .build()))
                .build();

        // when
        MessageConverterContext<String> context = MessageConverterContext.<String>builder()
                .facets(singletonList(facet1))
                .build();
        converter.before(context, outputStream);
        converter.after(context, outputStream);

        // then
        ObjectMapper om = new ObjectMapper();
        ResponseType response = om.readValue(outputStream.toString(), ResponseType.class);

        assertThat(response.getResults(), is(emptyList()));
        List<ReponseFacet> facets = response.getFacets();
        assertThat(facets, hasSize(1));

        assertThat(facets, contains(ReponseFacet.builder()
                                            .label(label1)
                                            .name(id1)
                                            .allowMultipleSelection(multipleSelection)
                                            .values(asList(ResponseFacetItem.builder()
                                                                   .count(facet1Item1Count)
                                                                   .label(facet1Item1Label)
                                                                   .value(facet1Item1Value)
                                                                   .build(),
                                                           ResponseFacetItem.builder()
                                                                   .count(facet1Item2Count)
                                                                   .label(facet1Item2Label)
                                                                   .value(facet1Item2Value)
                                                                   .build()))
                                            .build()));
    }

    // TODO: 08/11/18 test entity writing after flow has changed

    // class used to allow access methods before and after to test it properly...
    private static class UniProtKBJsonMessageConverterTester extends UniProtKBJsonMessageConverter{

        @Override
        protected void before(MessageConverterContext context, OutputStream outputStream) throws IOException {
            super.before(context,outputStream);
        }

        @Override
        protected void after(MessageConverterContext context, OutputStream outputStream) throws IOException {
            super.after(context,outputStream);
        }

    }

    @Data
    private static class ResponseType {
        private List<String> results;
        private List<ReponseFacet> facets;
    }

    @Data
    @Builder
    private static class ReponseFacet {
        private String label;

        private String name;

        private boolean allowMultipleSelection;

        private List<ResponseFacetItem> values;
    }

    @Data
    @Builder
    private static class ResponseFacetItem {
        private String label;

        private String value;

        private Long count;
    }
}