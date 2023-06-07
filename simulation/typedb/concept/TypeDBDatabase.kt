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
import com.vaticle.typedb.iam.simulation.typedb.Labels.DATABASE
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.benchmark.framework.common.seed.RandomSource

data class TypeDBDatabase(val name: String) : TypeDBObject(DATABASE, NAME, name) {
    companion object {
        fun initialise(seedData: SeedData, randomSource: RandomSource): TypeDBDatabase {
            val name = randomSource.choose(seedData.nouns)
            return TypeDBDatabase(name)
        }
    }
}