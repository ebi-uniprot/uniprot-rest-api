package org.uniprot.api.rest.validation.error;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

/**
 * Error response entity that provide error message details
 *
 * @author lgonzales
 */
@XmlRootElement
public class ErrorInfo {
    private final String url;
    private final List<String> messages;

    private ErrorInfo() {
        this("", Collections.emptyList());
    }

    public ErrorInfo(String url, List<String> messages) {
        assert url != null : "Error URL cannot be null";
        assert messages != null : "Error messages cannot be null";

        this.url = url;
        this.messages = messages;
    }

    @XmlElement
    public String getUrl() {
        return url;
    }

    @XmlElement
    public List<String> getMessages() {
        return messages;
    }
}
