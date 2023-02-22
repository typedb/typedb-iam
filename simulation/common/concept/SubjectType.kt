package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels as Labels

enum class SubjectType(val label: String, val type: String, val generable: Boolean) {
    PERSON(Labels.PERSON, Labels.USER, true),
    BUSINESS_UNIT(Labels.BUSINESS_UNIT, Labels.USER_GROUP, false),
    USER_ROLE(Labels.USER_ROLE, Labels.USER_GROUP, false),
    USER_ACCOUNT(Labels.USER_ACCOUNT, Labels.USER_GROUP, true)
}