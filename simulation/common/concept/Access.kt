package com.vaticle.typedb.iam.simulation.common.concept

data class Access(val accessedObject: Object, val validAction: Action)