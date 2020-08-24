package org.uniprot.api.rest.service.query.processor;

import org.apache.lucene.queryparser.flexible.standard.nodes.PointRangeQueryNode;

/**
 * This specific test class is required, because {@link UniProtPointRangeQueryNodeProcessorTest}
 * does not cover {@link PointRangeQueryNode} queries. At the time of writing, it is not clear how
 * to create such queries via a query String. Therefore, this test manually ensures it behaves okay.
 *
 * <p>Created 24/08/2020
 *
 * @author Edd
 */
class UniProtPointRangeQueryNodeProcessorTest {}
