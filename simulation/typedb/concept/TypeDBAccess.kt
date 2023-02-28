package com.vaticle.typedb.iam.simulation.typedb.concept

data class TypeDBAccess(val accessedObject: TypeDBObject, val validAction: TypeDBAction)