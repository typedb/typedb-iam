package com.vaticle.typedb.iam.simulation.common.concept

data class Permission(val permittedSubject: Subject, val permittedAccess: Access)