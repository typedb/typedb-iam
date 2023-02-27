package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels.APPLICATION
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME

data class Application(val name: String) {
    fun asObject(): Object {
        return Object(APPLICATION, NAME, name)
    }
}