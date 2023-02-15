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
import com.vaticle.typedb.simulation.common.seed.RandomSource

class Person(company: Company, seedData: SeedData, randomSource: RandomSource) {
    private val gender = initialiseGender(randomSource)
    private val firstName = initialiseFirstName(gender, seedData, randomSource)
    private val lastName = initialiseLastName(seedData, randomSource)
    val name = "$firstName $lastName"
    val email = "${firstName?.lowercase()}.${lastName?.lowercase()}@${company.domainName}.com"

    private fun initialiseGender(randomSource: RandomSource): String {
        return randomSource.choose(listOf("male", "female"))
    }

    private fun initialiseFirstName(gender:String, seedData: SeedData, randomSource: RandomSource): String? {
        val percentile = randomSource.nextInt(NAME_PERCENTILE_SCALE * MAX_NAME_PERCENTILE)
        val names = when (gender) {
            "male" -> seedData.maleNames
            "female" -> seedData.femaleNames
            else -> randomSource.choose(listOf(seedData.maleNames, seedData.femaleNames))
        }

        names.forEach {
            if ((NAME_PERCENTILE_SCALE * it["percentile"] as Float).toInt() <= percentile) {
                return it["value"] as String
            }
        }

        return null
    }

    private fun initialiseLastName(seedData: SeedData, randomSource: RandomSource): String? {
        val percentile = randomSource.nextInt(NAME_PERCENTILE_SCALE * MAX_NAME_PERCENTILE)

        seedData.lastNames.forEach {
            if ((NAME_PERCENTILE_SCALE * it["percentile"] as Float).toInt() <= percentile) {
                return it["value"] as String
            }
        }

        return null
    }

    companion object {
        const val MAX_NAME_PERCENTILE = 90
        const val NAME_PERCENTILE_SCALE = 1000
    }
}