package uk.ac.ebi.uniprot.uuw.advanced.search.http.context;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

/**
 * Created 07/09/18
 *
 * @author Edd
 */
@Builder
@Data
// TODO: 31/10/18 TEST this class, and asCopy -- seems default values with lombok not working. 
public class MessageConverterContext<T> {
    private FileType fileType = FileType.FILE;
    private MediaType contentType;
    private Stream<T> entities;
    private Stream<String> entityIds;
    private MessageConverterContextFactory.Resource resource;
    private String fields;

    public MessageConverterContext<T> asCopy() {
        return MessageConverterContext.<T>builder()
                .contentType(this.contentType)
                .entities(this.entities)
                .resource(this.resource)
                .fileType(this.fileType == null? FileType.FILE : this.fileType)
                .fields(this.fields)
                .entityIds(this.entityIds)
                .build();
    }
}
