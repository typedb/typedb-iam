package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.iam.simulation.typedb.Labels.DATABASE
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.simulation.common.seed.RandomSource

data class TypeDBDatabase(val name: String) : TypeDBObject(DATABASE, NAME, name) {
    companion object {
        fun initialise(seedData: SeedData, randomSource: RandomSource): TypeDBDatabase {
            val name = randomSource.choose(seedData.nouns)
            return TypeDBDatabase(name)
        }
    }
}