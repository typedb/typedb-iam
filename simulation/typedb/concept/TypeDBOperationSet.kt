package com.vaticle.typedb.iam.simulation.typedb.concept

data class TypeDBOperationSet(val name: String, val objectTypes: List<String>, val setMembers: List<String>)