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
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.ModelParams
import com.vaticle.typedb.simulation.Agent
import com.vaticle.typedb.simulation.common.driver.Client
import com.vaticle.typedb.simulation.common.driver.Session
import com.vaticle.typedb.simulation.common.driver.Transaction
import com.vaticle.typedb.simulation.common.seed.RandomSource
import java.time.LocalDateTime

abstract class CoupleFriendshipAgent<TX: Transaction> protected constructor(
    client: Client<Session<TX>>, context: Context
) : Agent<Country, TX, ModelParams>(client, context) {
    override val agentClass = CoupleFriendshipAgent::class.java
    override val partitions = context.seedData.countries

    override fun run(session: Session<TX>, partition: Country, random: RandomSource): List<Report> {
        // This agent targets the expense of the `put` operation of reasoning. More specifically the cost of `get` to
        // check whether a relation is pre-existing
        if (context.isReporting) throw RuntimeException("Reports are not comparable for reasoning agents.")
        session.reasoningTransaction().use { tx -> matchFriendships(tx, partition, context.today()) }
        return emptyList()
    }

    protected abstract fun matchFriendships(tx: TX, country: Country, marriageBirthDate: LocalDateTime)
}
