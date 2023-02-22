package com.vaticle.typedb.iam.simulation.common.concept

open class Entity(open val type: String, open val idType: String, open val idValue: String) {
    fun asSubject(): Subject {
        return Subject(type, idType, idValue)
    }

    fun asObject(): Object {
        return Object(type, idType, idValue)
    }

    fun asAction(): Action {
        return Action(type, idValue)
    }
}