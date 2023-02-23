package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.iam.simulation.common.Util.stringValue
import com.vaticle.typedb.iam.simulation.common.Util.typeLabel

open class Entity(open val type: String, open val idType: String, open val idValue: String) {
    constructor(type: Concept, idType: Concept, idValue: Concept): this(typeLabel(type), typeLabel(idType), stringValue(idValue))

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