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
import com.vaticle.typedb.simulation.common.DBClient

abstract class MaritalStatusAgent<SESSION> protected constructor(client: DBClient<SESSION>, context: Context) :
    Agent<Country, SESSION, ModelParams>(client, context) {
    override val agentClass = MaritalStatusAgent::class.java
    override val partitions = context.seedData.countries

    init {
        if (!context.isReporting) throw NotImplementedError("Reporting is not yet implemented in ${javaClass.simpleName}")
    }
}
