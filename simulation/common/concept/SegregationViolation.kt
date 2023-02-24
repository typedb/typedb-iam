package com.vaticle.typedb.iam.simulation.common.concept

data class SegregationViolation(val violatingSubject: Subject, val violatingObject: Object, val violatedPolicy: SegregationPolicy)