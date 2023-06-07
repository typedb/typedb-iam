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

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.agent.UserAgent
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.iam.simulation.typedb.Util.stringValue
import com.vaticle.typedb.iam.simulation.common.Util.printDuration
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.APPLICATION
import com.vaticle.typedb.iam.simulation.typedb.Labels.BUSINESS_UNIT
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.DIRECTORY
import com.vaticle.typedb.iam.simulation.typedb.Labels.EMAIL
import com.vaticle.typedb.iam.simulation.typedb.Labels.FULL_NAME
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
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_SET
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERSON
import com.vaticle.typedb.iam.simulation.typedb.Labels.SET_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.SET_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_ROLE
import com.vaticle.typedb.iam.simulation.typedb.agent.TypeDBAgentFactory
import com.vaticle.typedb.iam.simulation.common.concept.Application
import com.vaticle.typedb.iam.simulation.common.concept.BusinessUnit
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.common.concept.Operation
import com.vaticle.typedb.iam.simulation.common.concept.OperationSet
import com.vaticle.typedb.iam.simulation.common.concept.Person
import com.vaticle.typedb.iam.simulation.common.concept.UserRole
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACCESS
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACCESSED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.COLLECTION_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COLLECTION_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.ID
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COLLECTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALID_ACTION
import com.vaticle.typedb.iam.simulation.typedb.Util.cvar
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBObject
import com.vaticle.typedb.benchmark.framework.common.seed.RandomSource
import com.vaticle.typedb.benchmark.framework.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.insert
import com.vaticle.typeql.lang.TypeQL.match
import com.vaticle.typeql.lang.TypeQL.rel
import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths
import java.time.Instant
import kotlin.streams.toList

class TypeDBSimulation private constructor(client: TypeDBClient, context: Context):
    com.vaticle.typedb.benchmark.framework.typedb.TypeDBSimulation<Context>(client, context, TypeDBAgentFactory(client, context)) {

    override val agentPackage = UserAgent::class.java.packageName
    override val name = "IAM"
    // TODO: Update this filepath
    override val schemaFiles = listOf(SCHEMA_FILE_1, SCHEMA_FILE_2, SCHEMA_FILE_3).map { Paths.get(it).toFile() }
    private val options = TypeDBOptions.core().infer(true)

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
        if (context.model.createDemoConcepts) initDemoConcepts(nativeSession, context.seedData.companies)
        initAccesses(nativeSession, context.seedData.companies)
        LOGGER.info("TypeDB initialisation of world simulation data ended in: {}", printDuration(start, Instant.now()))
    }

    private fun initCompanies(session: TypeDBSession, companies: List<Company>) {
        companies.parallelStream().forEach { company ->
            session.transaction(WRITE).use { tx ->
                tx.query().insert(
                    insert(
                        cvar().isa(COMPANY).has(NAME, company.name),
                        cvar().eq(company.name).isa(PARENT_COMPANY_NAME),
                    )
                )

                tx.commit()
            }
        }
    }

    private fun initPersons(session: TypeDBSession, companies: List<Company>, seedData: SeedData, randomSource: RandomSource) {
        companies.parallelStream().forEach { company ->
            repeat(100) {
                val person = Person.initialise(company, seedData, randomSource)
                session.transaction(WRITE).use { tx ->
                    tx.query().insert(
                        match(
                            cvar(C).isa(COMPANY).has(NAME, company.name),
                        ).insert(
                            cvar(P).isa(PERSON).has(FULL_NAME, person.name).has(EMAIL, person.email),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(P)).isa(COMPANY_MEMBERSHIP),
                        )
                    )

                    tx.commit()
                }
            }
        }
    }

    private fun initBusinessUnits(session: TypeDBSession, companies: List<Company>, businessUnits: List<BusinessUnit>, randomSource: RandomSource) {
        companies.parallelStream().forEach { company ->
            val persons: List<Person>

            session.transaction(READ, options).use { tx ->
                persons = tx.query().match(
                    match(
                        cvar(P).isa(PERSON)
                            .has(FULL_NAME, cvar(P_NAME))
                            .has(EMAIL, cvar(P_EMAIL))
                            .has(PARENT_COMPANY_NAME, company.name),
                    )
                ).toList().map { Person(stringValue(it[P_NAME]), stringValue(it[P_EMAIL])) }
            }

            businessUnits.parallelStream().forEach { businessUnit ->
                val person = randomSource.choose(persons)

                session.transaction(WRITE).use { tx ->
                    tx.query().insert(
                        match(
                            cvar(C).isa(COMPANY).has(NAME, company.name),
                            cvar(P).isa(PERSON).has(EMAIL, person.email),
                        ).insert(
                            cvar(B).isa(BUSINESS_UNIT).has(NAME, businessUnit.name),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(B)).isa(COMPANY_MEMBERSHIP),
                            rel(OWNED_GROUP, cvar(B)).rel(GROUP_OWNER, cvar(P)).isa(GROUP_OWNERSHIP),
                        )
                    )

                    tx.commit()
                }
            }
        }
    }

    private fun initUserRoles(session: TypeDBSession, companies: List<Company>, userRoles: List<UserRole>, randomSource: RandomSource) {
        companies.parallelStream().forEach { company ->
            val persons: List<Person>

            session.transaction(READ, options).use { tx ->
                persons = tx.query().match(
                    match(
                        cvar(P).isa(PERSON)
                            .has(FULL_NAME, cvar(P_NAME))
                            .has(EMAIL, cvar(P_EMAIL))
                            .has(PARENT_COMPANY_NAME, company.name),
                    )
                ).toList().map { Person(stringValue(it[P_NAME]), stringValue(it[P_EMAIL])) }
            }

            userRoles.parallelStream().forEach { userRole ->
                val person = randomSource.choose(persons)

                session.transaction(WRITE).use { tx ->
                    tx.query().insert(
                        match(
                            cvar(C).isa(COMPANY).has(NAME, company.name),
                            cvar(P).isa(PERSON).has(EMAIL, person.email),
                        ).insert(
                            cvar(R).isa(USER_ROLE).has(NAME, userRole.name),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(R)).isa(COMPANY_MEMBERSHIP),
                            rel(OWNED_GROUP, cvar(R)).rel(GROUP_OWNER, cvar(P)).isa(GROUP_OWNERSHIP),
                        )
                    )

                    tx.commit()
                }
            }
        }
    }

    private fun initApplications(session: TypeDBSession, companies: List<Company>, applications: List<Application>, randomSource: RandomSource) {
        companies.parallelStream().forEach { company ->
            val persons: List<Person>

            session.transaction(READ, options).use { tx ->
                persons = tx.query().match(
                    match(
                        cvar(P).isa(PERSON)
                            .has(FULL_NAME, cvar(P_NAME))
                            .has(EMAIL, cvar(P_EMAIL))
                            .has(PARENT_COMPANY_NAME, company.name)
                    )
                ).toList().map { Person(stringValue(it[P_NAME]), stringValue(it[P_EMAIL])) }
            }

            applications.parallelStream().forEach { application ->
                val person = randomSource.choose(persons)

                session.transaction(WRITE).use { tx ->
                    tx.query().insert(
                        match(
                            cvar(C).isa(COMPANY).has(NAME, company.name),
                            cvar(P).isa(PERSON).has(EMAIL, person.email),
                        ).insert(
                            cvar(A).isa(APPLICATION).has(NAME, application.name).has(OBJECT_TYPE, APPLICATION),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                            rel(OWNED_OBJECT, cvar(A)).rel(OBJECT_OWNER, cvar(P)).isa(OBJECT_OWNERSHIP),
                        )
                    )

                    tx.commit()
                }
            }
        }
    }

    private fun initDirectories(session: TypeDBSession, companies: List<Company>, randomSource: RandomSource) {
        companies.parallelStream().forEach { company ->
            val persons: List<Person>

            session.transaction(READ, options).use { tx ->
                persons = tx.query().match(
                    match(
                        cvar(P).isa(PERSON)
                            .has(FULL_NAME, cvar(P_NAME))
                            .has(EMAIL, cvar(P_EMAIL))
                            .has(PARENT_COMPANY_NAME, company.name)
                    )
                ).toList().map { Person(stringValue(it[P_NAME]), stringValue(it[P_EMAIL])) }
            }

            val person = randomSource.choose(persons)

            session.transaction(WRITE).use { tx ->
                tx.query().insert(
                    match(
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(P).isa(PERSON).has(EMAIL, person.email),
                    ).insert(
                        cvar(D).isa(DIRECTORY).has(PATH, ROOT).has(OBJECT_TYPE, DIRECTORY),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(D)).isa(COMPANY_MEMBERSHIP),
                        rel(OWNED_OBJECT, cvar(D)).rel(OBJECT_OWNER, cvar(P)).isa(OBJECT_OWNERSHIP),
                    )
                )

                tx.commit()
            }
        }
    }

    private fun initOperations(session: TypeDBSession, companies: List<Company>, operations: List<Operation>) {
        companies.parallelStream().forEach { company ->
            operations.parallelStream().forEach { operation ->
                session.transaction(WRITE).use { tx ->
                    tx.query().insert(
                        match(
                            cvar(C).isa(COMPANY).has(NAME, company.name),
                        ).insert(
                            cvar(O).isa(OPERATION).has(ACTION_NAME, operation.name),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                        )
                    )

                    tx.commit()
                }
            }
        }
        operations.parallelStream().forEach { operation ->
            operation.objectTypes.parallelStream().forEach { objectType ->
                session.transaction(WRITE).use { tx ->
                    tx.query().insert(
                        match(
                            cvar(O).isa(OPERATION).has(ACTION_NAME, operation.name),
                        ).insert(
                            cvar(O).has(OBJECT_TYPE, objectType),
                        )
                    )

                    tx.commit()
                }
            }
        }
    }

    private fun initOperationSets(session: TypeDBSession, companies: List<Company>, operationSets: List<OperationSet>) {
        companies.parallelStream().forEach { company ->
            operationSets.parallelStream().forEach { operationSet ->
                session.transaction(WRITE).use { tx ->
                    tx.query().insert(
                        match(
                            cvar(C).isa(COMPANY).has(NAME, company.name),
                        ).insert(
                            cvar(S).isa(OPERATION_SET).has(ACTION_NAME, operationSet.name),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                        )
                    )

                    tx.commit()
                }
            }
        }
        operationSets.parallelStream().forEach { operationSet ->
            operationSet.objectTypes.parallelStream().forEach { objectType ->
                session.transaction(WRITE).use { tx ->
                    tx.query().insert(
                        match(
                            cvar(O).isa(OPERATION_SET).has(ACTION_NAME, operationSet.name),
                        ).insert(
                            cvar(O).has(OBJECT_TYPE, objectType),
                        )
                    )

                    tx.commit()
                }
            }
        }
        companies.parallelStream().forEach { company ->
            operationSets.parallelStream().forEach { operationSet ->
                operationSet.setMembers.parallelStream().forEach { setMember ->
                    session.transaction(WRITE).use { tx ->
                        tx.query().insert(
                            match(
                                cvar(S).isa(OPERATION_SET).has(ACTION_NAME, operationSet.name),
                                cvar(A).isa(ACTION).has(ACTION_NAME, setMember),
                                cvar(C).isa(COMPANY).has(NAME, company.name),
                                rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                                rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                            ).insert(
                                rel(PARENT_SET, cvar(S)).rel(SET_MEMBER, cvar(A)).isa(SET_MEMBERSHIP),
                            )
                        )

                        tx.commit()
                    }
                }
            }
        }
    }

    private fun initAccesses(session: TypeDBSession, companies: List<Company>) {
        companies.parallelStream().forEach { company ->
            val objects: List<TypeDBObject>

            session.transaction(READ, options).use { tx ->
                objects = tx.query().match(
                    match(
                        cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                        cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                        cvar(O_TYPE).sub(OBJECT),
                        cvar(O_ID_TYPE).sub(ID)
                    )
                ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
            }

            objects.parallelStream().forEach { obj ->
                session.transaction(WRITE).use { tx ->
                    tx.query().insert(
                        match(
                            cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                            cvar(A).isa(ACTION).has(OBJECT_TYPE, obj.type),
                        ).insert(
                            rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS)
                        )
                    )

                    tx.commit()
                }
            }
        }
    }

    private fun initDemoConcepts(session: TypeDBSession, companies: List<Company>) {
        if (companies.size > 1) throw IllegalStateException("Cannot create demo concepts in a simulation with multiple companies.")
        val company = companies.firstOrNull { it.name == VATICLE }
            ?: throw IllegalStateException("Cannot create demo concepts without company \"Vaticle\".")
        if (ROOT != "root") throw IllegalStateException("Cannot create demo concepts without \"root\" as root directory.")

        val person = Person("Gavin", "Harrison", company)

        session.transaction(WRITE).use { tx ->
            tx.query().insert(
                match(
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                ).insert(
                    cvar(P).isa(PERSON).has(FULL_NAME, person.name).has(EMAIL, person.email),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(P)).isa(COMPANY_MEMBERSHIP),
                )
            )

            tx.query().insert(
                match(
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(D1).isa(DIRECTORY).has(PATH, ROOT),
                    cvar(P).isa(PERSON).has(EMAIL, person.email),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(D1)).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(P)).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    cvar(D2).isa(DIRECTORY).has(PATH, "${ROOT}/typedb").has(OBJECT_TYPE, DIRECTORY),
                    cvar(D3).isa(DIRECTORY)
                        .has(PATH, "${ROOT}/typedb/src")
                        .has(OBJECT_TYPE, DIRECTORY)
                        .has(OBJECT_TYPE, REPOSITORY),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(D2)).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(D3)).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COLLECTION, cvar(D1)).rel(COLLECTION_MEMBER, cvar(D2)).isa(COLLECTION_MEMBERSHIP),
                    rel(PARENT_COLLECTION, cvar(D2)).rel(COLLECTION_MEMBER, cvar(D3)).isa(COLLECTION_MEMBERSHIP),
                    rel(OWNED_OBJECT, cvar(D2)).rel(OBJECT_OWNER, cvar(P)).isa(OBJECT_OWNERSHIP),
                    rel(OWNED_OBJECT, cvar(D3)).rel(OBJECT_OWNER, cvar(P)).isa(OBJECT_OWNERSHIP),
                )
            )

            tx.commit()
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private const val SCHEMA_FILE_1 = "iam-schema-core-concepts.tql"
        private const val SCHEMA_FILE_2 = "iam-schema-core-rules.tql"
        private const val SCHEMA_FILE_3 = "iam-schema-ext-simulation.tql"
        private const val A = "a"
        private const val B = "b"
        private const val C = "c"
        private const val D = "d"
        private const val D1 = "d1"
        private const val D2 = "d2"
        private const val D3 = "d3"
        private const val O = "o"
        private const val O_ID = "o-id"
        private const val O_ID_TYPE = "o-id-type"
        private const val O_TYPE = "o-type"
        private const val P = "p"
        private const val P_EMAIL = "p-email"
        private const val P_NAME = "p-name"
        private const val R = "r"
        private const val REPOSITORY = "repository"
        private const val ROOT = "root"
        private const val S = "s"
        private const val VATICLE = "Vaticle"

        fun core(address: String, context: Context): TypeDBSimulation {
            return TypeDBSimulation(TypeDBClient.core(address, context.dbName), context).apply { init() }
        }

        fun cluster(address: String, context: Context): TypeDBSimulation {
            return TypeDBSimulation(TypeDBClient.cluster(address, context.dbName), context).apply { init() }
        }
    }
}
