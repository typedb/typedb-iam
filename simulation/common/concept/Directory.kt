package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.simulation.common.seed.RandomSource

data class Directory(val path: String) {
    companion object {
        fun initialise(parent: Directory, seedData: SeedData, randomSource: RandomSource): Directory {
            val noun = randomSource.choose(seedData.nouns)
            val filepath = "${parent.path}/${noun}"
            return Directory(filepath)
        }
    }
}