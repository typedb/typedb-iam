package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.simulation.common.seed.RandomSource

data class Database(val name: String) {
    companion object {
        fun initialise(seedData: SeedData, randomSource: RandomSource): Database {
            val name = randomSource.choose(seedData.nouns)
            return Database(name)
        }
    }
}