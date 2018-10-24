package uk.ac.ebi.uniprot.uuw.advanced.search.http.context;

/**
 * Created 06/09/18
 *
 * @author Edd
 */
public class JsonMessageConverterContext extends MessageConverterContext implements HeaderFooterContext {
    private String header;
    private String footer;

    @Override
    public MessageConverterContext asCopy() {
        JsonMessageConverterContext copy = new JsonMessageConverterContext();
        copy.setContentType(this.getContentType());
        copy.setFileType(this.getFileType());
        copy.setResource(this.getResource());
        copy.setEntities(this.getEntities());
        copy.setFooter(this.footer);
        copy.setHeader(this.header);
        return copy;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JsonMessageConverterContext that = (JsonMessageConverterContext) o;

        if (header != null ? !header.equals(that.header) : that.header != null) return false;
        return footer != null ? footer.equals(that.footer) : that.footer == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (header != null ? header.hashCode() : 0);
        result = 31 * result + (footer != null ? footer.hashCode() : 0);
        return result;
    }
}
