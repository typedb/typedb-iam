package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels.BUSINESS_UNIT
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME

data class BusinessUnit(val name: String) {
    fun asSubject(): Subject {
        return Subject(BUSINESS_UNIT, NAME, name)
    }
}