package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.iam.simulation.typedb.Util.stringValue
import com.vaticle.typedb.iam.simulation.typedb.Util.typeLabel

open class TypeDBObject(override val type: String, override val idType: String, override val idValue: String): TypeDBEntity(type, idType, idValue) {
    constructor(type: Concept, idType: Concept, idValue: Concept): this(typeLabel(type), typeLabel(idType), stringValue(idValue))
}