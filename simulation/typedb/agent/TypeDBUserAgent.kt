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
package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.iam.simulation.agent.UserAgent
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACCESS
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACCESSED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.CHANGE_REQUEST
import com.vaticle.typedb.iam.simulation.typedb.Labels.COLLECTION_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COLLECTION_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.DATABASE
import com.vaticle.typedb.iam.simulation.typedb.Labels.ID
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_TYPE
import com.vaticle.typedb.iam.simulation.typedb.Labels.OPERATION
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COLLECTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMISSION
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMITTED_ACCESS
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMITTED_SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.REQUESTED_CHANGE
import com.vaticle.typedb.iam.simulation.typedb.Labels.REQUESTED_SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.REQUESTING_SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.ROOT_COLLECTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALIDITY
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALID_ACTION
import com.vaticle.typedb.iam.simulation.typedb.Util.cvar
import com.vaticle.typedb.iam.simulation.typedb.Util.getRandomEntity
import com.vaticle.typedb.iam.simulation.typedb.Util.stringValue
import com.vaticle.typedb.iam.simulation.typedb.concept.*
import com.vaticle.typedb.benchmark.framework.common.seed.RandomSource
import com.vaticle.typedb.benchmark.framework.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import kotlin.streams.toList

class TypeDBUserAgent(client: TypeDBClient, context: Context) : UserAgent<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions.core().infer(true)

    override fun createObject(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val objectType = randomSource.choose(TypeDBObjectType.values().asList().filter { it.generable })

        val reportList = when (objectType) {
            TypeDBObjectType.FILE -> createObject(session, company, randomSource, objectType)
            TypeDBObjectType.DIRECTORY -> createObject(session, company, randomSource, objectType)
            TypeDBObjectType.INTERFACE -> createObject(session, company, randomSource, objectType)
            TypeDBObjectType.APPLICATION -> throw IllegalArgumentException() // Applications are only created on database initialisation.
            TypeDBObjectType.RECORD -> createObject(session, company, randomSource, objectType)
            TypeDBObjectType.TABLE -> createObject(session, company, randomSource, objectType)
            TypeDBObjectType.DATABASE -> createDatabase(session, company, randomSource) // Databases have no parent collections so must be handled uniquely.
        }

        return reportList
    }

    override fun deleteObject(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val candidateObjects: List<TypeDBObject>

        session.transaction(READ, options).use { tx ->
            candidateObjects = tx.query().match(
                match(
                    cvar(O).isaX(cvar(O_TYPE))
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    not(cvar(O).has(ROOT_COLLECTION, true)),
                )
            ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
        }

        if (candidateObjects.isEmpty()) return listOf()
        val obj = randomSource.choose(candidateObjects)
        val members: List<TypeDBObject>

        session.transaction(READ, options).use { tx ->
            members = tx.query().match(
                match(
                    cvar(O).isa(obj.type).has(PARENT_COMPANY_NAME, company.name).has(obj.idType, obj.idValue),
                    cvar(O_MEMBER).isaX(cvar(O_MEMBER_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_MEMBER_ID)),
                    cvar(O_MEMBER_ID).isaX(cvar(O_MEMBER_ID_TYPE)),
                    cvar(O_MEMBER_TYPE).sub(OBJECT),
                    cvar(O_MEMBER_ID_TYPE).sub(ID),
                    rel(PARENT_COLLECTION, cvar(O)).rel(COLLECTION_MEMBER, cvar(O_MEMBER)).isa(COLLECTION_MEMBERSHIP),
                )
            ).toList().map { TypeDBObject(it[O_MEMBER_TYPE], it[O_MEMBER_ID_TYPE], it[O_MEMBER_ID]) }
        }

        val objectsToDelete = members + listOf(obj)

        session.transaction(WRITE).use { tx ->
            objectsToDelete.parallelStream().forEach { obj ->
                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(A).rel(ACCESSED_OBJECT, cvar(O)).isa(ACCESS),
                        cvar(P).rel(PERMITTED_ACCESS, cvar(A)).isa(PERMISSION),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(P).isa(PERMISSION)
                    )
                )

                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(A).rel(ACCESSED_OBJECT, cvar(O)).isa(ACCESS),
                        cvar(R).rel(REQUESTED_CHANGE, cvar(A)).isa(CHANGE_REQUEST),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(R).isa(CHANGE_REQUEST),
                    )
                )

                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(A).rel(ACCESSED_OBJECT, cvar(O)).isa(ACCESS),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(A).isa(ACCESS),
                    )
                )

                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(OW).rel(OWNED_OBJECT, cvar(O)).isa(OBJECT_OWNERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(OW).isa(OBJECT_OWNERSHIP),
                    )
                )

                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(ME).rel(cvar(O)).isa(COLLECTION_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(ME).isa(COLLECTION_MEMBERSHIP),
                    )
                )

                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(ME).rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(O).isa(obj.type),
                        cvar(ME).isa(COMPANY_MEMBERSHIP),
                    )
                )
            }

            tx.commit()
        }

        return listOf()
    }

    override fun attemptAccess(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val candidateObjects: List<TypeDBObject>
        val candidateOperations: List<TypeDBAction>
        val subject = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf()

        session.transaction(READ, options).use { tx ->
            candidateObjects = tx.query().match(
                match(
                    cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    rel(ACCESSED_OBJECT, cvar(O)).isa(ACCESS),
                )
            ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
        }

        if (candidateObjects.isEmpty()) return listOf()
        val obj = randomSource.choose(candidateObjects)

        session.transaction(READ, options).use { tx ->
            candidateOperations = tx.query().match(
                match(
                    cvar(A).isa(OPERATION).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A_NAME)),
                    cvar(O).isa(obj.type).has(PARENT_COMPANY_NAME, company.name).has(obj.idType, obj.idValue),
                    rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                )
            ).toList().map { TypeDBAction(OPERATION, stringValue(it[A_NAME])) }
        }

        val action = randomSource.choose(candidateOperations)
        val matches: List<ConceptMap>

        session.transaction(READ, options).use { tx ->
            matches = tx.query().match(
                match(
                    cvar(S).isa(subject.type).has(PARENT_COMPANY_NAME, company.name).has(subject.idType, subject.idValue),
                    cvar(O).isa(obj.type).has(PARENT_COMPANY_NAME, company.name).has(obj.idType, obj.idValue),
                    cvar(A).isa(action.type).has(PARENT_COMPANY_NAME, company.name).has(action.idType, action.idValue),
                    cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                    rel(PERMITTED_SUBJECT, cvar(S)).rel(PERMITTED_ACCESS, cvar(AC)).isa(PERMISSION).has(VALIDITY, true)
                )
            ).toList()
        }

        val result = matches.isNotEmpty()
        return listOf()
    }

    override fun submitChangeRequest(
        session: TypeDBSession,
        company: Company,
        randomSource: RandomSource
    ): List<Report> {
        val requestingSubject = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf()
        val requestedSubject = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf()
        val candidateObjects: List<TypeDBObject>

        session.transaction(READ, options).use { tx ->
            candidateObjects = tx.query().match(
                match(
                    cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    rel(ACCESSED_OBJECT, cvar(O)).isa(ACCESS),
                )
            ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
        }

        if (candidateObjects.isEmpty()) return listOf()
        val obj = randomSource.choose(candidateObjects)
        val candidateOperations: List<TypeDBAction>

        session.transaction(READ, options).use { tx ->
            candidateOperations = tx.query().match(
                match(
                    cvar(A).isa(OPERATION).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A_NAME)),
                    cvar(O).isa(obj.type).has(PARENT_COMPANY_NAME, company.name).has(obj.idType, obj.idValue),
                    rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                )
            ).toList().map { TypeDBAction(OPERATION, stringValue(it[A_NAME])) }
        }

        val action = randomSource.choose(candidateOperations)

        session.transaction(WRITE).use { tx ->
            tx.query().insert(
                match(
                    cvar(S_REQUESTING).isa(requestingSubject.type).has(requestingSubject.idType, requestingSubject.idValue),
                    cvar(S_REQUESTED).isa(requestedSubject.type).has(requestedSubject.idType, requestedSubject.idValue),
                    cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                    cvar(A).isa(action.type).has(action.idType, action.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S_REQUESTING)).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S_REQUESTED)).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    rel(REQUESTING_SUBJECT, cvar(S_REQUESTING)).rel(REQUESTED_SUBJECT, cvar(S_REQUESTED)).rel(REQUESTED_CHANGE, cvar(AC)).isa(CHANGE_REQUEST)
                )
            )

            tx.commit()
        }

        return listOf()
    }

    private fun createObject(
        session: TypeDBSession,
        company: Company,
        randomSource: RandomSource,
        objectType: TypeDBObjectType
    ): List<Report> {
        val parentType = when (objectType) {
            TypeDBObjectType.FILE -> TypeDBObjectType.DIRECTORY
            TypeDBObjectType.DIRECTORY -> TypeDBObjectType.DIRECTORY
            TypeDBObjectType.INTERFACE -> TypeDBObjectType.APPLICATION
            TypeDBObjectType.APPLICATION -> throw IllegalArgumentException()
            TypeDBObjectType.RECORD -> TypeDBObjectType.TABLE
            TypeDBObjectType.TABLE -> TypeDBObjectType.DATABASE
            TypeDBObjectType.DATABASE -> throw IllegalArgumentException()
        }

        val parent = getRandomEntity(session, company, randomSource, parentType.label)?.asObject() ?: return listOf()

        val obj = when (objectType) {
            TypeDBObjectType.FILE -> TypeDBFile.initialise(parent.idValue, context.seedData, randomSource)
            TypeDBObjectType.DIRECTORY -> TypeDBDirectory.initialise(parent.idValue, context.seedData, randomSource)
            TypeDBObjectType.INTERFACE -> TypeDBInterface.initialise(parent.idValue, context.seedData, randomSource)
            TypeDBObjectType.APPLICATION -> throw IllegalArgumentException()
            TypeDBObjectType.RECORD -> TypeDBRecord.initialise(randomSource)
            TypeDBObjectType.TABLE -> TypeDBTable.initialise(parent.idValue, context.seedData, randomSource)
            TypeDBObjectType.DATABASE -> throw IllegalArgumentException()
        }

        session.transaction(READ, options).use { tx ->
            if (
                tx.query().match(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue).has(PARENT_COMPANY_NAME, company.name)
                    )
                ).toList().isNotEmpty()
            ) return listOf()
        }

        val owner = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf()
        val validActions: List<TypeDBAction>

        session.transaction(READ, options).use { tx ->
            validActions = tx.query().match(
                match(
                    cvar(A).isaX(cvar(A_TYPE))
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(OBJECT_TYPE, obj.type)
                        .has(ACTION_NAME, cvar(A_NAME)),
                    cvar(A_TYPE).sub(ACTION)
                )
            ).toList().map { TypeDBAction(it[A_TYPE], it[A_NAME]) }
        }

        session.transaction(WRITE).use { tx ->
            tx.query().insert(
                match(
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(CO).isa(parent.type).has(parent.idType, parent.idValue),
                    cvar(S).isa(owner.type).has(owner.idType, owner.idValue),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(CO)).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    cvar(O).isa(obj.type).has(obj.idType, obj.idValue).has(OBJECT_TYPE, obj.type),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COLLECTION, cvar(CO)).rel(COLLECTION_MEMBER, cvar(O)).isa(COLLECTION_MEMBERSHIP),
                    rel(OWNED_OBJECT, cvar(O)).rel(OBJECT_OWNER, cvar(S)).isa(OBJECT_OWNERSHIP),
                )
            )

            validActions.parallelStream().forEach { action ->
                tx.query().insert(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(A).isa(action.type).has(action.idType, action.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                    ).insert(
                        rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                    )
                )
            }

            tx.commit()
        }

        return listOf()
    }

    private fun createDatabase(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val database = TypeDBDatabase.initialise(context.seedData, randomSource)

        session.transaction(READ, options).use { tx ->
            if (
                tx.query().match(
                    match(
                        cvar(O).isa(DATABASE).has(NAME, database.name).has(PARENT_COMPANY_NAME, company.name)
                    )
                ).toList().isNotEmpty()
            ) return listOf()
        }

        val owner = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf()
        val validActions: List<TypeDBAction>

        session.transaction(READ, options).use { tx ->
            validActions = tx.query().match(
                match(
                    cvar(A).isaX(cvar(A_TYPE))
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(OBJECT_TYPE, DATABASE)
                        .has(ACTION_NAME, cvar(A_NAME)),
                    cvar(A_TYPE).sub(ACTION)
                )
            ).toList().map { TypeDBAction(it[A_TYPE], it[A_NAME]) }
        }

        session.transaction(WRITE).use { tx ->
            tx.query().insert(
                match(
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(S).isa(owner.type).has(owner.idType, owner.idValue),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    cvar(O).isa(DATABASE).has(NAME, database.name),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                    rel(OWNED_OBJECT, cvar(O)).rel(OBJECT_OWNER, cvar(S)).isa(OBJECT_OWNERSHIP),
                )
            )

            validActions.parallelStream().forEach { action ->
                tx.query().insert(
                    match(
                        cvar(O).isa(DATABASE).has(NAME, database.name),
                        cvar(A).isa(action.type).has(action.idType, action.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                    ).insert(
                        rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                    )
                )
            }

            tx.commit()
        }

        return listOf()
    }

    companion object {
        private const val A = "a"
        private const val AC = "ac"
        private const val A_NAME = "a-name"
        private const val A_TYPE = "a-type"
        private const val C = "c"
        private const val CO = "co"
        private const val ME = "me"
        private const val O = "o"
        private const val OW = "ow"
        private const val O_ID = "o-id"
        private const val O_ID_TYPE = "o-id-type"
        private const val O_MEMBER = "o-member"
        private const val O_MEMBER_ID = "o-member-id"
        private const val O_MEMBER_ID_TYPE = "o-member-id-type"
        private const val O_MEMBER_TYPE = "o-member-type"
        private const val O_TYPE = "o-type"
        private const val P = "p"
        private const val R = "r"
        private const val S = "s"
        private const val S_REQUESTED = "s-requested"
        private const val S_REQUESTING = "s-requesting"
    }
}
