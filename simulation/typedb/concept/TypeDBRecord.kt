package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels.NUMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.RECORD
import com.vaticle.typedb.simulation.common.seed.RandomSource

data class TypeDBRecord(val number: String) {
    fun asObject(): TypeDBObject {
        return TypeDBObject(RECORD, NUMBER, number)
    }

    companion object {
        fun initialise(randomSource: RandomSource): TypeDBRecord {
            val number = randomSource.nextInt(1000000000).toString()
            return TypeDBRecord(number)
        }
    }
}