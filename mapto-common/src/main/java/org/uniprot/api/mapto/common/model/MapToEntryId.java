package org.uniprot.api.mapto.common.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class MapToEntryId implements Serializable {
    private static final long serialVersionUID = 1457666439792103463L;
    private String id;
}
