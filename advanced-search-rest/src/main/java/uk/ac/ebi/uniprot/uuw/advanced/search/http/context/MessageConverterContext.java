package uk.ac.ebi.uniprot.uuw.advanced.search.http.context;

import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.request.SearchRequestDTO;

import java.util.stream.Stream;

/**
 * Created 07/09/18
 *
 * @author Edd
 */
public class MessageConverterContext {
    private FileType fileType;
    private MediaType contentType;
    private Stream<?> entities;
    private MessageConverterContextFactory.Resource resource;
    private SearchRequestDTO requestDTO;

    public MessageConverterContext asCopy() {
        MessageConverterContext context = new MessageConverterContext();
        context.resource = this.resource;
        context.entities = this.entities;
        context.contentType = this.contentType;
        context.fileType = this.fileType;
        context.requestDTO = this.requestDTO;
        return context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageConverterContext that = (MessageConverterContext) o;

        if (fileType != that.fileType) return false;
        if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null) return false;
        if (entities != null ? !entities.equals(that.entities) : that.entities != null) return false;
        if (resource != that.resource) return false;
        return requestDTO != null ? requestDTO.equals(that.requestDTO) : that.requestDTO == null;
    }

    @Override
    public int hashCode() {
        int result = fileType != null ? fileType.hashCode() : 0;
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + (entities != null ? entities.hashCode() : 0);
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        result = 31 * result + (requestDTO != null ? requestDTO.hashCode() : 0);
        return result;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public void setContentType(MediaType contentType) {
        this.contentType = contentType;
    }

    public Stream<?> getEntities() {
        return entities;
    }

    public void setEntities(Stream<?> entities) {
        this.entities = entities;
    }

    MessageConverterContextFactory.Resource getResource() {
        return resource;
    }

    public void setResource(MessageConverterContextFactory.Resource resource) {
        this.resource = resource;
    }

    public SearchRequestDTO getRequestDTO() {
        return requestDTO;
    }

    public void setRequestDTO(SearchRequestDTO requestDTO) {
        this.requestDTO = requestDTO;
    }
}
