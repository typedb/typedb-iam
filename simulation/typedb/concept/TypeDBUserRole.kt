package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_ROLE

data class TypeDBUserRole(val name: String) {
    fun asSubject(): TypeDBSubject {
        return TypeDBSubject(USER_ROLE, NAME, name)
    }
}