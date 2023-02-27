package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.TABLE
import com.vaticle.typedb.simulation.common.seed.RandomSource

data class Table(val name: String) {
    fun asObject(): Object {
        return Object(TABLE, NAME, name)
    }

    companion object {
        fun initialise(database: Database, seedData: SeedData, randomSource: RandomSource): Table {
            val adjective = randomSource.choose(seedData.adjectives)
            val name = "${adjective}_${database.name}"
            return Table(name)
        }

        fun initialise(databaseName: String, seedData: SeedData, randomSource: RandomSource): Table {
            val adjective = randomSource.choose(seedData.adjectives)
            val name = "${adjective}_${databaseName}"
            return Table(name)
        }
    }
}