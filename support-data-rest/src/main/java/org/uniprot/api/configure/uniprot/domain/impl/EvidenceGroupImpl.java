package org.uniprot.api.configure.uniprot.domain.impl;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.api.configure.uniprot.domain.EvidenceGroup;
import org.uniprot.api.configure.uniprot.domain.EvidenceItem;

@Data
@NoArgsConstructor
public class EvidenceGroupImpl implements EvidenceGroup {

	private String groupName;
	private List<EvidenceItem> items = new ArrayList<>();

}
