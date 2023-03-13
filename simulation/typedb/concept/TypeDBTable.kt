package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.TABLE
import com.vaticle.typedb.simulation.common.seed.RandomSource

data class TypeDBTable(val name: String): TypeDBObject(TABLE, NAME, name) {
    companion object {
        fun initialise(database: TypeDBDatabase, seedData: SeedData, randomSource: RandomSource): TypeDBTable {
            val adjective = randomSource.choose(seedData.adjectives)
            val name = "${adjective}_${database.name}"
            return TypeDBTable(name)
        }

        fun initialise(databaseName: String, seedData: SeedData, randomSource: RandomSource): TypeDBTable {
            val adjective = randomSource.choose(seedData.adjectives)
            val name = "${adjective}_${databaseName}"
            return TypeDBTable(name)
        }
    }
}