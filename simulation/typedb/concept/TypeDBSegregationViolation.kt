package com.vaticle.typedb.iam.simulation.typedb.concept

data class TypeDBSegregationViolation(val violatingSubject: TypeDBSubject, val violatingObject: TypeDBObject, val violatedPolicy: TypeDBSegregationPolicy)