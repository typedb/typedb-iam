package com.vaticle.typedb.iam.simulation.typedb.concept

data class SegregationViolation(val violatingSubject: Subject, val violatingObject: Object, val violatedPolicy: SegregationPolicy)