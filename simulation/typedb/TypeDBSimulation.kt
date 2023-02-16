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
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.agent.User
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.iam.simulation.common.Util.printDuration
import com.vaticle.typedb.iam.simulation.common.concept.BusinessUnit
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.common.concept.Person
import com.vaticle.typedb.iam.simulation.common.concept.UserRole
import com.vaticle.typedb.iam.simulation.typedb.Labels.BUSINESS_UNIT
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.EMAIL
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERSON
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_GROUP
import com.vaticle.typedb.iam.simulation.typedb.agent.TypeDBAgentFactory
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.insert
import com.vaticle.typeql.lang.TypeQL.match
import com.vaticle.typeql.lang.TypeQL.rel
import com.vaticle.typeql.lang.TypeQL.`var`
import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths
import java.time.Instant
import kotlin.streams.toList

class TypeDBSimulation private constructor(client: TypeDBClient, context: Context):
    com.vaticle.typedb.simulation.typedb.TypeDBSimulation<Context>(client, context, TypeDBAgentFactory(client, context)) {

    // TODO: Add randomSource to context.
    override val agentPackage: String = User::class.java.packageName
    override val name = "IAM"
    override val schemaFile: File = Paths.get("define_schema.tql").toFile()

    override fun initData(nativeSession: TypeDBSession) {
        LOGGER.info("TypeDB initialisation of world simulation data started ...")
        val start = Instant.now()
        initCompanies(nativeSession, context.seedData.companies)
        initPersons(nativeSession, context.seedData.companies, context.seedData, randomSource)
        initBusinessUnits(nativeSession, context.seedData.companies, context.seedData.businessUnits, randomSource)
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

    private fun initPersons(session: TypeDBSession, companies: List<Company>, seedData: SeedData, randomSource: RandomSource) {
        companies.parallelStream().forEach { company: Company ->
            repeat(100) {
                val person = Person.initialise(company, seedData, randomSource)
                session.transaction(WRITE).use { transaction ->
                    transaction.query().insert(
                        match(
                            `var`(C).isa(COMPANY).has(NAME, company.name)
                        ).insert(
                            `var`(P).isa(PERSON).has(NAME, person.name).has(EMAIL, person.email),
                            rel(COMPANY, C).rel(COMPANY_MEMBER, P).isa(COMPANY_MEMBERSHIP)
                        )
                    )
                }
            }
        }
    }

    private fun initBusinessUnits(session: TypeDBSession, companies: List<Company>, businessUnits: List<BusinessUnit>, randomSource: RandomSource) {
        companies.parallelStream().forEach { company: Company ->
            val persons: List<Person>

            session.transaction(READ).use { transaction ->
                persons = transaction.query().match(
                    match(
                        `var`(P).isa(PERSON).has(NAME, P_NAME).has(EMAIL, P_EMAIL).has(PARENT_COMPANY, company.name)
                    ).get(
                        P_NAME, P_EMAIL
                    )
                ).toList().map { Person(it[P_NAME].asAttribute().asString().value, it[P_EMAIL].asAttribute().asString().value) }
            }

            businessUnits.parallelStream().forEach { businessUnit: BusinessUnit ->
                val person = randomSource.choose(persons)

                session.transaction(WRITE).use { transaction ->
                    transaction.query().insert(
                        match(
                            `var`(C).isa(COMPANY).has(NAME, company.name),
                            `var`(P).isa(PERSON).has(EMAIL, person.email)
                        ).insert(
                            `var`(B).isa(BUSINESS_UNIT).has(NAME, businessUnit.name),
                            rel(COMPANY, C).rel(COMPANY_MEMBER, B).isa(COMPANY_MEMBERSHIP),
                            rel(USER_GROUP, B).rel(GROUP_OWNER, P).isa(GROUP_OWNERSHIP)
                        )
                    )
                }
            }
        }
    }

    private fun initUserRoles(session: TypeDBSession, companies: List<Company>, userRoles: List<UserRole>, randomSource: RandomSource) {
        companies.parallelStream().forEach { company: Company ->
            val persons: List<Person>

            session.transaction(READ).use { transaction ->
                persons = transaction.query().match(
                    match(
                        `var`(P).isa(PERSON).has(NAME, P_NAME).has(EMAIL, P_EMAIL).has(PARENT_COMPANY, company.name)
                    ).get(
                        P_NAME, P_EMAIL
                    )
                ).toList().map { Person(it[P_NAME].asAttribute().asString().value, it[P_EMAIL].asAttribute().asString().value) }
            }

            userRoles.parallelStream().forEach { userRole: UserRole ->
                session.transaction(REA)
            }
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private const val B = "b"
        private const val C = "c"
        private const val P = "p"
        private const val P_EMAIL = "p-email"
        private const val P_NAME = "p-name"

        fun core(address: String, context: Context): TypeDBSimulation {
            return TypeDBSimulation(TypeDBClient.core(address, context.dbName), context).apply { init() }
        }

        fun cluster(address: String, context: Context): TypeDBSimulation {
            return TypeDBSimulation(TypeDBClient.cluster(address, context.dbName), context).apply { init() }
        }
    }
}
