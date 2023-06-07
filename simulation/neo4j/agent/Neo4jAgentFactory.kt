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
package com.vaticle.typedb.iam.simulation.neo4j.agent

import com.vaticle.typedb.iam.simulation.agent.*
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.benchmark.framework.neo4j.Neo4jClient

class Neo4jAgentFactory(client: Neo4jClient, context: Context) : AgentFactory<Neo4jClient>(client, context) {

    companion object {
        fun unsupportedReasoningAgentException(agentName: String): UnsupportedOperationException {
            return UnsupportedOperationException("$agentName requires reasoning, which is not supported by Neo4j")
        }
    }

    override fun user(client: Neo4jClient, context: Context): UserAgent<*> {
        TODO("Not yet implemented")
    }

    override fun owner(client: Neo4jClient, context: Context): OwnerAgent<*> {
        TODO("Not yet implemented")
    }

    override fun supervisor(client: Neo4jClient, context: Context): SupervisorAgent<*> {
        TODO("Not yet implemented")
    }

    override fun policyManager(client: Neo4jClient, context: Context): PolicyManagerAgent<*> {
        TODO("Not yet implemented")
    }

    override fun sysAdmin(client: Neo4jClient, context: Context): SysAdminAgent<*> {
        TODO("Not yet implemented")
    }
}
