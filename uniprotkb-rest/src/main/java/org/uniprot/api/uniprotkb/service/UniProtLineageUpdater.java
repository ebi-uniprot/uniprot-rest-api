package org.uniprot.api.uniprotkb.service;

import org.uniprot.core.uniprotkb.UniProtkbEntry;

/**
 * @author jluo
 * @date: 16 Oct 2019
 */
public interface UniProtLineageUpdater {
    UniProtkbEntry updateLineage(UniProtkbEntry entry);
}
