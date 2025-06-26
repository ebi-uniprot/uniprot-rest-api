package org.uniprot.api.idmapping.common.request;

import org.uniprot.api.rest.request.PageRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created 25/02/2021
 *
 * @author Edd
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class IdMappingPageRequest extends PageRequest {}
