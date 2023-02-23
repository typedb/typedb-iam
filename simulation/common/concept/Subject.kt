package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.iam.simulation.common.Util.stringValue
import com.vaticle.typedb.iam.simulation.common.Util.typeLabel

data class Subject(override val type: String, override val idType: String, override val idValue: String): Entity(type, idType, idValue) {
    constructor(type: Concept, idType: Concept, idValue: Concept): this(typeLabel(type), typeLabel(idType), stringValue(idValue))
}