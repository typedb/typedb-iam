package com.vaticle.typedb.iam.simulation.typedb.concept

data class ChangeRequest(val requestingSubject: Subject, val requestedSubject: Subject, val requestedAccess: Access)