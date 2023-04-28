package org.uniprot.api.rest.download.model;

/**
 * Created 23/02/2021
 *
 * @author Edd
 */
public enum JobStatus {
    NEW,
    RUNNING,
    PROCESSING, // while AA embedding consumer is running
    UNFINISHED, // rest code sets before handing over to AA embedding consumer
    FINISHED,
    ERROR
}
