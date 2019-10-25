package org.uniprot.api.uniprotkb.service;

import org.uniprot.core.uniprot.UniProtEntry;

/**
 *
 * @author jluo
 * @date: 16 Oct 2019
 *
*/

public interface UniProtLineageUpdater {
	UniProtEntry updateLineage(UniProtEntry entry);
}

