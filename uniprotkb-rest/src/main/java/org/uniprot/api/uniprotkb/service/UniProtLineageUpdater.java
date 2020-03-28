package org.uniprot.api.uniprotkb.service;

import org.uniprot.core.uniprotkb.UniProtKBEntry;

/**
 * @author jluo
 * @date: 16 Oct 2019
 */
public interface UniProtLineageUpdater {
    UniProtKBEntry updateLineage(UniProtKBEntry entry);
}
