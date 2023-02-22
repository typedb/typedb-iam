package com.vaticle.typedb.iam.simulation.common.concept

data class Object(override val type: String, override val idType: String, override val idValue: String): Entity(type, idType, idValue)