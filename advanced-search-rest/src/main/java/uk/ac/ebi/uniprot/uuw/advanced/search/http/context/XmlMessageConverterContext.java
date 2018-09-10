package uk.ac.ebi.uniprot.uuw.advanced.search.http.context;

import java.util.function.Function;

/**
 * Created 06/09/18
 *
 * @author Edd
 */
public class XmlMessageConverterContext<S, T> extends MessageConverterContext {
    private String header;
    private String footer;
    private String context;
    private Function<S, T> converter;

    @Override
    public MessageConverterContext asCopy() {
        XmlMessageConverterContext<S, T> copy = new XmlMessageConverterContext<>();
        copy.setContentType(this.getContentType());
        copy.setFileType(this.getFileType());
        copy.setResource(this.getResource());
        copy.setEntities(this.getEntities());
        copy.setConverter(this.converter);
        copy.setContext(this.context);
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

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Function<S, T> getConverter() {
        return converter;
    }

    public void setConverter(Function<S, T> converter) {
        this.converter = converter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        XmlMessageConverterContext<?, ?> that = (XmlMessageConverterContext<?, ?>) o;

        if (header != null ? !header.equals(that.header) : that.header != null) return false;
        if (footer != null ? !footer.equals(that.footer) : that.footer != null) return false;
        if (context != null ? !context.equals(that.context) : that.context != null) return false;
        return converter != null ? converter.equals(that.converter) : that.converter == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (header != null ? header.hashCode() : 0);
        result = 31 * result + (footer != null ? footer.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        result = 31 * result + (converter != null ? converter.hashCode() : 0);
        return result;
    }
}
