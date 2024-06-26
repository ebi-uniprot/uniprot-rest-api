package org.uniprot.api.async.download.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class JobSubmitFeedback {
    private final boolean allowed;
    private String message;
}
