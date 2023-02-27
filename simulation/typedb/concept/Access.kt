package com.vaticle.typedb.iam.simulation.typedb.concept

data class Access(val accessedObject: Object, val validAction: Action)