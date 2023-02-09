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
package com.vaticle.typedb.iam.simulation.agent

import com.vaticle.typedb.iam.simulation.common.concept.Country
import com.vaticle.typedb.iam.simulation.common.concept.Gender
import com.vaticle.typedb.iam.simulation.common.concept.Gender.FEMALE
import com.vaticle.typedb.iam.simulation.common.concept.Gender.MALE
import com.vaticle.typedb.iam.simulation.common.concept.Marriage
import com.vaticle.typedb.iam.simulation.common.concept.Person
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.ModelParams
import com.vaticle.typedb.simulation.Agent
import com.vaticle.typedb.simulation.common.driver.Client
import com.vaticle.typedb.simulation.common.driver.Session
import com.vaticle.typedb.simulation.common.driver.Transaction
import com.vaticle.typedb.simulation.common.seed.RandomSource
import java.time.LocalDateTime
import java.util.Comparator.comparing
import java.util.stream.Collectors.toList
import java.util.stream.Stream

abstract class MarriageAgent<TX: Transaction> protected constructor(client: Client<Session<TX>>, context: Context) :
    Agent<Country, TX, ModelParams>(client, context) {
    override val agentClass = MarriageAgent::class.java
    override val partitions = context.seedData.countries

    override fun run(session: Session<TX>, partition: Country, random: RandomSource): List<Report> {
        val reports = mutableListOf<Report>()
        session.writeTransaction().use { tx ->
            val partnerBirthDate = context.today().minusYears(context.model.ageOfAdulthood.toLong())
            val women = matchPartner(tx, partition, partnerBirthDate, FEMALE).sorted(comparing { it.email }).collect(toList())
            val men = matchPartner(tx, partition, partnerBirthDate, MALE).sorted(comparing { it.email }).collect(toList())
            random.randomPairs(women, men).forEach { (woman, man) ->
                val licence = woman.email + man.email
                val inserted = insertMarriage(tx, woman.email, man.email, licence, context.today())
                if (context.isReporting) {
                    requireNotNull(inserted)
                    reports.add(Report(
                        input = listOf(woman.email, man.email, licence, context.today()),
                        output = listOf(inserted)
                    ))
                } else assert(inserted == null)
            }
            tx.commit()
        }
        return reports
    }

    protected abstract fun matchPartner(tx: TX, country: Country, birthDate: LocalDateTime, gender: Gender): Stream<Person>

    protected abstract fun insertMarriage(
        tx: TX, wifeEmail: String, husbandEmail: String, marriageLicence: String, marriageDate: LocalDateTime
    ): Marriage?
}
