package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels.APPLICATION
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME

data class TypeDBApplication(val name: String): TypeDBObject(APPLICATION, NAME, name)