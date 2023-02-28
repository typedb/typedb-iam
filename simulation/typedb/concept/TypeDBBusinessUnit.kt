package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels.BUSINESS_UNIT
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME

data class TypeDBBusinessUnit(val name: String) {
    fun asSubject(): TypeDBSubject {
        return TypeDBSubject(BUSINESS_UNIT, NAME, name)
    }
}