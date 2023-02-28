package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.iam.simulation.typedb.Util.stringValue
import com.vaticle.typedb.iam.simulation.typedb.Util.typeLabel

open class TypeDBEntity(open val type: String, open val idType: String, open val idValue: String) {
    constructor(type: Concept, idType: Concept, idValue: Concept): this(typeLabel(type), typeLabel(idType), stringValue(idValue))

    fun asSubject(): TypeDBSubject {
        return TypeDBSubject(type, idType, idValue)
    }

    fun asObject(): TypeDBObject {
        return TypeDBObject(type, idType, idValue)
    }

    fun asAction(): TypeDBAction {
        return TypeDBAction(type, idValue)
    }
}