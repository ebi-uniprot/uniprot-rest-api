package uk.ac.ebi.uniprot.uuw.advanced.search.http.context;

import org.springframework.http.MediaType;

import java.util.stream.Stream;

/**
 * Created 07/09/18
 *
 * @author Edd
 */
public class MessageConverterContext<T> {
    private FileType fileType = FileType.FILE;
    private MediaType contentType;
    private Stream<T> entities;
    private MessageConverterContextFactory.Resource resource;
    private String fields;

    public MessageConverterContext<T> asCopy() {
        MessageConverterContext<T> context = new MessageConverterContext<>();
        context.resource = this.resource;
        context.entities = this.entities;
        context.contentType = this.contentType;
        context.fileType = this.fileType;
        context.fields = this.fields;
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
        return fields != null ? fields.equals(that.fields) : that.fields == null;
    }

    @Override
    public int hashCode() {
        int result = fileType != null ? fileType.hashCode() : 0;
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + (entities != null ? entities.hashCode() : 0);
        result = 31 * result + (resource != null ? resource.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
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

    public Stream<T> getEntities() {
        return entities;
    }

    public void setEntities(Stream<T> entities) {
        this.entities = entities;
    }
    public void setEntities(Stream<T> entities, Class<T> type) {
        this.entities = entities;
    }

    MessageConverterContextFactory.Resource getResource() {
        return resource;
    }

    public void setResource(MessageConverterContextFactory.Resource resource) {
        this.resource = resource;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }
}
