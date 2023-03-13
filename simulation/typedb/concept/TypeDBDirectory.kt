package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.iam.simulation.typedb.Labels.DIRECTORY
import com.vaticle.typedb.iam.simulation.typedb.Labels.PATH
import com.vaticle.typedb.simulation.common.seed.RandomSource

data class TypeDBDirectory(val path: String): TypeDBObject(DIRECTORY, PATH, path) {
    companion object {
        fun initialise(parent: TypeDBDirectory, seedData: SeedData, randomSource: RandomSource): TypeDBDirectory {
            val noun = randomSource.choose(seedData.nouns)
            val filepath = "${parent.path}/${noun}"
            return TypeDBDirectory(filepath)
        }

        fun initialise(parentPath: String, seedData: SeedData, randomSource: RandomSource): TypeDBDirectory {
            val noun = randomSource.choose(seedData.nouns)
            val filepath = "${parentPath}/${noun}"
            return TypeDBDirectory(filepath)
        }
    }
}