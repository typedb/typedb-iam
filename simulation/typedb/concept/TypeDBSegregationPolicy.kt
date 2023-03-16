package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.iam.simulation.typedb.Util.stringValue

data class TypeDBSegregationPolicy(val action1: TypeDBAction, val action2: TypeDBAction, val name: String) {
    constructor(action1: TypeDBAction, action2: TypeDBAction, name: Concept): this(action1, action2, stringValue(name))
}