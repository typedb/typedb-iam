package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION_NAME

data class Action(override val type: String, override val idValue: String): Entity(type, ACTION_NAME, idValue)