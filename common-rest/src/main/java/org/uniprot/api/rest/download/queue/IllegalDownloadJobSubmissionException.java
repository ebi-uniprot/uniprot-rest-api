package org.uniprot.api.rest.download.queue;

import lombok.Getter;

@Getter
public class IllegalDownloadJobSubmissionException extends RuntimeException {
    private static final long serialVersionUID = 5756120882260405553L;
    private final String jobId;

    public IllegalDownloadJobSubmissionException(String jobId, String message) {
        super(message);
        this.jobId = jobId;
    }
}
