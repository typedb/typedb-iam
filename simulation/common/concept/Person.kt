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
import com.vaticle.typedb.benchmark.framework.common.seed.RandomSource

data class Person(val name: String, val email: String) {
    constructor(firstName: String, lastName: String, company: Company): this(
        name = "$firstName $lastName",
        email = "${firstName.lowercase()}.${lastName.lowercase()}@${company.domainName}.com",
    )

    companion object {
        private const val MAX_NAME_PERCENTILE = 90
        private const val NAME_PERCENTILE_SCALE = 1000

        fun initialise(company: Company, seedData: SeedData, randomSource: RandomSource): Person {
            val gender = randomSource.choose(listOf("male", "female"))
            val firstName = initialiseFirstName(gender, seedData, randomSource)
            val lastName = initialiseLastName(seedData, randomSource)
            val name = "$firstName $lastName"
            val email = "${firstName.lowercase()}.${lastName.lowercase()}@${company.domainName}.com"
            return Person(name, email)
        }

        private fun initialiseFirstName(gender:String, seedData: SeedData, randomSource: RandomSource): String {
            val names = when (gender) {
                "male" -> seedData.maleNames
                "female" -> seedData.femaleNames
                else -> randomSource.choose(listOf(seedData.maleNames, seedData.femaleNames))
            }

            return randomSource.choose(names)["value"] as String
        }

        private fun initialiseLastName(seedData: SeedData, randomSource: RandomSource): String {
            return randomSource.choose(seedData.lastNames)["value"] as String
        }

        private fun initialiseFirstNameByPercentile(gender:String, seedData: SeedData, randomSource: RandomSource): String? {
            val percentile = randomSource.nextInt(NAME_PERCENTILE_SCALE * MAX_NAME_PERCENTILE)
            val names = when (gender) {
                "male" -> seedData.maleNames
                "female" -> seedData.femaleNames
                else -> randomSource.choose(listOf(seedData.maleNames, seedData.femaleNames))
            }

            names.forEach {
                if ((NAME_PERCENTILE_SCALE * it["percentile"] as Double).toInt() >= percentile) {
                    return it["value"] as String
                }
            }

            return null
        }

        private fun initialiseLastNameByPercentile(seedData: SeedData, randomSource: RandomSource): String? {
            val percentile = randomSource.nextInt(NAME_PERCENTILE_SCALE * MAX_NAME_PERCENTILE)

            seedData.lastNames.forEach {
                if ((NAME_PERCENTILE_SCALE * it["percentile"] as Double).toInt() >= percentile) {
                    return it["value"] as String
                }
            }

            return null
        }
    }
}