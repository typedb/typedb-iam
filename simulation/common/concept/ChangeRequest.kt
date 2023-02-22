package com.vaticle.typedb.iam.simulation.common.concept

data class ChangeRequest(val requestingSubject: Subject, val requestedSubject: Subject, val requestedAccess: Access)