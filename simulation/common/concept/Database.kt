package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.iam.simulation.typedb.Labels.DATABASE
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.simulation.common.seed.RandomSource

data class Database(val name: String) {
    fun asObject(): Object {
        return com.vaticle.typedb.iam.simulation.common.concept.Object(DATABASE, NAME, name)
    }

    companion object {
        fun initialise(seedData: SeedData, randomSource: RandomSource): Database {
            val name = randomSource.choose(seedData.nouns)
            return Database(name)
        }
    }
}