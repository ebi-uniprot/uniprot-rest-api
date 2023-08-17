package org.uniprot.api.rest.validation;

import javax.validation.GroupSequence;
import javax.validation.groups.Default;

/**
 * When validating CustomConstraintGroupSequence, first the Default group is validated. If all the
 * data passes validation, then the CustomConstraintGroup group is validated. If a constraint is
 * part of both the Default and the CustomConstraintGroup groups, the constraint is validated as
 * part of the Default group and will not be validated on the subsequent CustomConstraintGroup pass.
 * To use on an object @Validated(CustomConstraintGroupSequence.class)
 */
@GroupSequence({Default.class, CustomConstraintGroup.class})
public interface CustomConstraintGroupSequence {}
