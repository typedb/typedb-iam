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
import com.vaticle.typedb.iam.simulation.typedb.Labels.DIRECTORY
import com.vaticle.typedb.iam.simulation.typedb.Labels.PATH
import com.vaticle.typedb.benchmark.framework.common.seed.RandomSource

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