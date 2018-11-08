package uk.ac.ebi.uniprot.rest.output.context;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.common.repository.search.facet.Facet;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Created 07/09/18
 *
 * @author Edd
 */
@Builder
@Data
public class MessageConverterContext<T> {
    public static final FileType DEFAULT_FILE_TYPE = FileType.FILE;
    @Builder.Default
    private FileType fileType = DEFAULT_FILE_TYPE;
    private MediaType contentType;
    private Stream<T> entities;
    private Stream<String> entityIds;
    private MessageConverterContextFactory.Resource resource;
    private String fields;
    private Collection<Facet> facets;

    MessageConverterContext<T> asCopy() {
        return MessageConverterContext.<T>builder()
                .contentType(this.contentType)
                .entities(this.entities)
                .resource(this.resource)
                .fileType(this.fileType)
                .fields(this.fields)
                .entityIds(this.entityIds)
                .facets(this.facets)
                .build();
    }
}
