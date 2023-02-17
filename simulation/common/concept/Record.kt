package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.simulation.common.seed.RandomSource

data class Record(val number: Int, val table: Table) {
    companion object {
        fun initialise(table: Table, randomSource: RandomSource): Record {
            val number = randomSource.nextInt(1000000000)
            return Record(number, table)
        }
    }
}