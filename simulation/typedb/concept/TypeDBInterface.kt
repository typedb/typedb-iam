/*
 * Copyright (C) 2022 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.iam.simulation.typedb.Labels.INTERFACE
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.benchmark.framework.common.seed.RandomSource

data class TypeDBInterface(val name: String): TypeDBObject(INTERFACE, NAME, name) {
    companion object {
        fun initialise(application: TypeDBApplication, seedData: SeedData, randomSource: RandomSource): TypeDBInterface {
            val adjective = randomSource.choose(seedData.adjectives)
            val noun = randomSource.choose(seedData.nouns)
            val name = "${application.name} ${adjective} ${noun}"
            return TypeDBInterface(name)
        }

        fun initialise(applicationName: String, seedData: SeedData, randomSource: RandomSource): TypeDBInterface {
            val adjective = randomSource.choose(seedData.adjectives)
            val noun = randomSource.choose(seedData.nouns)
            val name = "${applicationName} ${adjective} ${noun}"
            return TypeDBInterface(name)
        }
    }
}