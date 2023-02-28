package com.vaticle.typedb.iam.simulation.typedb.concept

data class TypeDBChangeRequest(val requestingSubject: TypeDBSubject, val requestedSubject: TypeDBSubject, val requestedAccess: TypeDBAccess)