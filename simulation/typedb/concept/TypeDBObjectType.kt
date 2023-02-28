package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels as Labels

enum class TypeDBObjectType(val label: String, val type: String, val generable: Boolean) {
    FILE(Labels.FILE, Labels.RESOURCE, true),
    DIRECTORY(Labels.DIRECTORY, Labels.RESOURCE_COLLECTION, true),
    INTERFACE(Labels.INTERFACE, Labels.RESOURCE, true),
    APPLICATION(Labels.APPLICATION, Labels.RESOURCE_COLLECTION, false),
    RECORD(Labels.RECORD, Labels.RESOURCE, true),
    TABLE(Labels.TABLE, Labels.RESOURCE_COLLECTION, true),
    DATABASE(Labels.DATABASE, Labels.RESOURCE_COLLECTION, true)
}