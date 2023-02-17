package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.simulation.common.seed.RandomSource

data class Table(val name: String, val database: Database) {
    companion object {
        fun initialise(database: Database, seedData: SeedData, randomSource: RandomSource): Table {
            val adjective = randomSource.choose(seedData.adjectives)
            val name = "${adjective}_${database.name}"
            return Table(name, database)
        }
    }
}