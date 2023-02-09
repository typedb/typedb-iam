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
import com.vaticle.typedb.iam.simulation.common.concept.Marriage
import com.vaticle.typedb.iam.simulation.common.concept.Parenthood
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

abstract class ParenthoodAgent<TX: Transaction> protected constructor(client: Client<Session<TX>>, context: Context) :
    Agent<Country, TX, ModelParams>(client, context) {
    override val agentClass = ParenthoodAgent::class.java
    override val partitions = context.seedData.countries

    override fun run(session: Session<TX>, partition: Country, random: RandomSource): List<Report> {
        val reports: MutableList<Report> = ArrayList()
        session.writeTransaction().use { tx ->
            val marriageDate = context.today().minusYears(context.model.yearsBeforeParenthood.toLong())
            val marriages = matchMarriages(tx, partition, marriageDate).sorted(comparing { it.licence }).collect(toList())
            val newBorns = matchNewborns(tx, partition, context.today()).sorted(comparing { it.email }).collect(toList())
            val parenthoods = random.randomAllocation(marriages, newBorns)
            parenthoods.forEach { (marriage, person) ->
                val wife = marriage.wife.email
                val husband = marriage.husband.email
                val child = person.email
                val inserted = insertParenthood(tx, wife, husband, child)
                if (context.isReporting) {
                    requireNotNull(inserted)
                    reports.add(Report(input = listOf(wife, husband, child), output = listOf(inserted)))
                } else assert(inserted == null)
            }
            tx.commit()
        }
        return reports
    }

    protected abstract fun matchNewborns(tx: TX, country: Country, today: LocalDateTime): Stream<Person>
    protected abstract fun matchMarriages(tx: TX, country: Country, marriageDate: LocalDateTime): Stream<Marriage>
    protected abstract fun insertParenthood(tx: TX, motherEmail: String, fatherEmail: String, childEmail: String): Parenthood?
}
