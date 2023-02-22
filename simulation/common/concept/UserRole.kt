package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_ROLE

data class UserRole(val name: String) {
    fun asSubject(): Subject {
        return Subject(USER_ROLE, NAME, name)
    }
}