package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.iam.simulation.typedb.Util.stringValue
import com.vaticle.typedb.iam.simulation.typedb.Util.typeLabel
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION_NAME

data class Action(override val type: String, override val idValue: String): Entity(type, ACTION_NAME, idValue) {
    constructor(type: Concept, idValue: Concept): this(typeLabel(type), stringValue(idValue))
}