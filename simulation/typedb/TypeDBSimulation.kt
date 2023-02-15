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
package com.vaticle.typedb.iam.simulation.typedb

import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.agent.AgentFactory
import com.vaticle.typedb.iam.simulation.agent.User
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.Util.printDuration
import com.vaticle.typedb.iam.simulation.common.concept.BusinessUnit
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.typedb.Labels.BUSINESS_UNIT
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.agent.TypeDBAgentFactory
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.insert
import com.vaticle.typeql.lang.TypeQL.match
import com.vaticle.typeql.lang.TypeQL.rel
import com.vaticle.typeql.lang.TypeQL.`var`
import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths
import java.time.Instant

class TypeDBSimulation private constructor(
    client: TypeDBClient, context: Context
) : com.vaticle.typedb.simulation.typedb.TypeDBSimulation<Context>(client, context, TypeDBAgentFactory(client, context)) {

    override val agentPackage: String = User::class.java.packageName

    override val name = "IAM"

    override val schemaFile: File = Paths.get("define_schema.tql").toFile()

    override fun initData(nativeSession: com.vaticle.typedb.client.api.TypeDBSession) {
        LOGGER.info("TypeDB initialisation of world simulation data started ...")
        val start = Instant.now()
        initCompanies(nativeSession, context.seedData.companies)
        LOGGER.info("TypeDB initialisation of world simulation data ended in: {}", printDuration(start, Instant.now()))
    }

    private fun initCompanies(session: TypeDBSession, companies: List<Company>) {
        companies.parallelStream().forEach { company: Company ->
            session.transaction(WRITE).use { transaction ->
                transaction.query().insert(
                    insert(
                        `var`().isa(COMPANY).has(NAME, company.name)
                    )
                )
                transaction.commit()
            }
        }
    }

    private fun initBusinessUnits(session: TypeDBSession, companies: List<Company>, businessUnits: List<BusinessUnit>) {
        companies.parallelStream().forEach { company: Company ->
            businessUnits.parallelStream().forEach { businessUnit: BusinessUnit ->
                session.transaction(WRITE).use { transaction ->
                    transaction.query().insert(
                        match(
                            `var`(C).isa(COMPANY).has(NAME, company.name)
                        ).insert(
                            `var`().isa(BUSINESS_UNIT).has()
                        )
                    )
                }
            }
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private const val C = "c"

        fun core(address: String, context: Context): TypeDBSimulation {
            return TypeDBSimulation(TypeDBClient.core(address, context.dbName), context).apply { init() }
        }

        fun cluster(address: String, context: Context): TypeDBSimulation {
            return TypeDBSimulation(TypeDBClient.cluster(address, context.dbName), context).apply { init() }
        }
    }
}
