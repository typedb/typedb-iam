package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels.APPLICATION
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME

data class TypeDBApplication(val name: String) {
    fun asObject(): TypeDBObject {
        return TypeDBObject(APPLICATION, NAME, name)
    }
}