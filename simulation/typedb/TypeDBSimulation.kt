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
import com.vaticle.typedb.iam.simulation.common.concept.*
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.APPLICATION
import com.vaticle.typedb.iam.simulation.typedb.Labels.BUSINESS_UNIT
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.DIRECTORY
import com.vaticle.typedb.iam.simulation.typedb.Labels.EMAIL
import com.vaticle.typedb.iam.simulation.typedb.Labels.PATH
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_TYPE
import com.vaticle.typedb.iam.simulation.typedb.Labels.OPERATION
import com.vaticle.typedb.iam.simulation.typedb.Labels.OPERATION_SET
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_GROUP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERSON
import com.vaticle.typedb.iam.simulation.typedb.Labels.SET_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.SET_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_ROLE
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

    override val agentPackage: String = User::class.java.packageName
    override val name = "IAM"
    override val schemaFile: File = Paths.get("define_schema.tql").toFile()

    override fun initData(nativeSession: TypeDBSession, randomSource: RandomSource) {
        LOGGER.info("TypeDB initialisation of world simulation data started ...")
        val start = Instant.now()
        initCompanies(nativeSession, context.seedData.companies)
        initPersons(nativeSession, context.seedData.companies, context.seedData, randomSource)
        initBusinessUnits(nativeSession, context.seedData.companies, context.seedData.businessUnits, randomSource)
        initUserRoles(nativeSession, context.seedData.companies, context.seedData.userRoles, randomSource)
        initApplications(nativeSession, context.seedData.companies, context.seedData.applications, randomSource)
        initDirectories(nativeSession, context.seedData.companies, randomSource)
        initOperations(nativeSession, context.seedData.companies, context.seedData.operations)
        initOperationSets(nativeSession, context.seedData.companies, context.seedData.operationSets)
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
                            rel(OWNED_GROUP, B).rel(GROUP_OWNER, P).isa(GROUP_OWNERSHIP)
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
                val person = randomSource.choose(persons)

                session.transaction(WRITE).use { transaction ->
                    transaction.query().insert(
                        match(
                            `var`(C).isa(COMPANY).has(NAME, company.name),
                            `var`(P).isa(PERSON).has(EMAIL, person.email)
                        ).insert(
                            `var`(R).isa(USER_ROLE).has(NAME, userRole.name),
                            rel(COMPANY, C).rel(COMPANY_MEMBER, R).isa(COMPANY_MEMBERSHIP),
                            rel(OWNED_GROUP, R).rel(GROUP_OWNER, P).isa(GROUP_OWNERSHIP)
                        )
                    )
                }
            }
        }
    }

    private fun initApplications(session: TypeDBSession, companies: List<Company>, applications: List<Application>, randomSource: RandomSource) {
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

            applications.parallelStream().forEach { application: Application ->
                val person = randomSource.choose(persons)

                session.transaction(WRITE).use { transaction ->
                    transaction.query().insert(
                        match(
                            `var`(C).isa(COMPANY).has(NAME, company.name),
                            `var`(P).isa(PERSON).has(EMAIL, person.email)
                        ).insert(
                            `var`(A).isa(APPLICATION).has(NAME, application.name),
                            rel(COMPANY, C).rel(COMPANY_MEMBER, A).isa(COMPANY_MEMBERSHIP),
                            rel(OWNED_OBJECT, A).rel(OBJECT_OWNER, P).isa(OBJECT_OWNERSHIP)
                        )
                    )
                }
            }
        }
    }

    private fun initDirectories(session: TypeDBSession, companies: List<Company>, randomSource: RandomSource) {
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

            val person = randomSource.choose(persons)

            session.transaction(WRITE).use { transaction ->
                transaction.query().insert(
                    match(
                        `var`(C).isa(COMPANY).has(NAME, company.name),
                        `var`(P).isa(PERSON).has(EMAIL, person.email)
                    ).insert(
                        `var`(D).isa(DIRECTORY).has(PATH, ROOT),
                        rel(COMPANY, C).rel(COMPANY_MEMBER, D).isa(COMPANY_MEMBERSHIP),
                        rel(OWNED_OBJECT, D).rel(OBJECT_OWNER, P).isa(OBJECT_OWNERSHIP)
                    )
                )
            }
        }
    }

    private fun initOperations(session: TypeDBSession, companies: List<Company>, operations: List<Operation>) {
        companies.parallelStream().forEach { company: Company ->
            operations.parallelStream().forEach { operation: Operation ->
                session.transaction(WRITE).use { transaction ->
                    transaction.query().insert(
                        match(
                            `var`(C).isa(COMPANY).has(NAME, company.name)
                        ).insert(
                            `var`(O).isa(OPERATION).has(NAME, operation.name),
                            rel(COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP)
                        )
                    )
                }
            }
        }
        operations.parallelStream().forEach { operation: Operation ->
            operation.objectTypes.parallelStream().forEach { objectType: String ->
                session.transaction(WRITE).use { transaction ->
                    transaction.query().insert(
                        match(
                            `var`(O).isa(OPERATION).has(NAME, operation.name)
                        ).insert(
                            `var`(O).has(OBJECT_TYPE, objectType)
                        )
                    )
                }
            }
        }
    }

    private fun initOperationSets(session: TypeDBSession, companies: List<Company>, operationSets: List<OperationSet>) {
        companies.parallelStream().forEach { company: Company ->
            operationSets.parallelStream().forEach { operationSet: OperationSet ->
                session.transaction(WRITE).use { transaction ->
                    transaction.query().insert(
                        match(
                            `var`(C).isa(COMPANY).has(NAME, company.name)
                        ).insert(
                            `var`(S).isa(OPERATION_SET).has(NAME, operationSet.name),
                            rel(COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP)
                        )
                    )
                }
            }
        }
        operationSets.parallelStream().forEach { operationSet: OperationSet ->
            operationSet.objectTypes.parallelStream().forEach { objectType: String ->
                session.transaction(WRITE).use { transaction ->
                    transaction.query().insert(
                        match(
                            `var`(O).isa(OPERATION_SET).has(NAME, operationSet.name)
                        ).insert(
                            `var`(O).has(OBJECT_TYPE, objectType)
                        )
                    )
                }
            }
        }
        companies.parallelStream().forEach { company: Company ->
            operationSets.parallelStream().forEach { operationSet: OperationSet ->
                operationSet.setMembers.parallelStream().forEach { setMember: String ->
                    session.transaction(WRITE).use { transaction ->
                        transaction.query().insert(
                            match(
                                `var`(S).isa(OPERATION_SET).has(NAME, operationSet.name).has(PARENT_COMPANY, company.name),
                                `var`(A).isa(ACTION).has(NAME, setMember).has(PARENT_COMPANY, company.name)
                            ).insert(
                                rel(OPERATION_SET, S).rel(SET_MEMBER, A).isa(SET_MEMBERSHIP)
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private const val A = "a"
        private const val B = "b"
        private const val C = "c"
        private const val D = "d"
        private const val O = "o"
        private const val P = "p"
        private const val P_EMAIL = "p-email"
        private const val P_NAME = "p-name"
        private const val R = "r"
        private const val S = "s"
        private const val ROOT = "root"

        fun core(address: String, context: Context): TypeDBSimulation {
            return TypeDBSimulation(TypeDBClient.core(address, context.dbName), context).apply { init() }
        }

        fun cluster(address: String, context: Context): TypeDBSimulation {
            return TypeDBSimulation(TypeDBClient.cluster(address, context.dbName), context).apply { init() }
        }
    }
}
