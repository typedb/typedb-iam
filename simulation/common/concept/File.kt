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
package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.iam.simulation.typedb.Labels.FILE
import com.vaticle.typedb.iam.simulation.typedb.Labels.PATH
import com.vaticle.typedb.simulation.common.seed.RandomSource

data class File(val path: String) {
    fun asObject(): Object {
        return com.vaticle.typedb.iam.simulation.common.concept.Object(FILE, PATH, path)
    }

    companion object {
        fun initialise(directory: Directory, seedData: SeedData, randomSource: RandomSource): File {
            val adjective = randomSource.choose(seedData.adjectives)
            val noun = randomSource.choose(seedData.nouns)
            val fileExtension = randomSource.choose(seedData.fileExtensions)
            val filepath = "${directory.path}/${adjective}_${noun}.${fileExtension}"
            return File(filepath)
        }

        fun initialise(directoryName: String, seedData: SeedData, randomSource: RandomSource): File {
            val adjective = randomSource.choose(seedData.adjectives)
            val noun = randomSource.choose(seedData.nouns)
            val fileExtension = randomSource.choose(seedData.fileExtensions)
            val filepath = "${directoryName}/${adjective}_${noun}.${fileExtension}"
            return File(filepath)
        }
    }
}