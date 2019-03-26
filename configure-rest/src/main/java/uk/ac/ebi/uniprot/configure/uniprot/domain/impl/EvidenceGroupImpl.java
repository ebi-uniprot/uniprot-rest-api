package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.EvidenceItem;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class EvidenceGroupImpl implements EvidenceGroup {

	private String groupName;
	private List<EvidenceItem> items = new ArrayList<>();

}
