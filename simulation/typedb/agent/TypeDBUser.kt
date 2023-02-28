package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.agent.User
import com.vaticle.typedb.iam.simulation.typedb.Util.stringValue
import com.vaticle.typedb.iam.simulation.typedb.Util.getRandomEntity
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
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBAction
import com.vaticle.typedb.iam.simulation.common.`object`.Company
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY_NAME
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBDatabase
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBDirectory
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBFile
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBInterface
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBObject
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBObjectType
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBRecord
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBTable
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import java.lang.IllegalArgumentException
import kotlin.streams.toList

class TypeDBUser(client: TypeDBClient, context:Context): User<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions.core().infer(true)

    override fun createObject(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val objectType = randomSource.choose(TypeDBObjectType.values().asList().filter { it.generable })

        when (objectType) {
            TypeDBObjectType.FILE -> createObject(session, company, randomSource, objectType)
            TypeDBObjectType.DIRECTORY -> createObject(session, company, randomSource, objectType)
            TypeDBObjectType.INTERFACE -> createObject(session, company, randomSource, objectType)
            TypeDBObjectType.APPLICATION -> throw IllegalArgumentException() // Applications are only created on database initialisation.
            TypeDBObjectType.RECORD -> createObject(session, company, randomSource, objectType)
            TypeDBObjectType.TABLE -> createObject(session, company, randomSource, objectType)
            TypeDBObjectType.DATABASE -> createDatabase(session, company, randomSource) // Databases have no parent collections so must be handled uniquely.
        }

        return listOf<Report>()
    }

    override fun deleteObject(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val candidateObjects: List<TypeDBObject>

        session.transaction(READ, options).use { transaction ->
            candidateObjects = transaction.query().match(
                match(
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(ROOT_COLLECTION, false)
                        .has(ID, `var`(O_ID)),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE)
                )
            ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
        }

        val `object` = randomSource.choose(candidateObjects)
        val members: List<TypeDBObject>

        session.transaction(READ, options).use { transaction ->
            members = transaction.query().match(
                match(
                    `var`(O).isa(`object`.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(`object`.idType, `object`.idValue),
                    `var`(O_MEMBER).isa(OBJECT)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(ID, `var`(O_MEMBER_ID)),
                    rel(PARENT_COLLECTION, O).rel(COLLECTION_MEMBER, O_MEMBER).isa(COLLECTION_MEMBERSHIP),
                    `var`(O_MEMBER).isaX(O_MEMBER_TYPE),
                    `var`(O_MEMBER_ID).isaX(O_MEMBER_ID_TYPE)
                )
            ).toList().map { TypeDBObject(it[O_MEMBER_TYPE], it[O_MEMBER_ID_TYPE], it[O_MEMBER_ID]) }
        }

        val objectsToDelete = members + listOf(`object`)

        session.transaction(WRITE, options).use { transaction ->
            objectsToDelete.parallelStream().forEach { `object` ->
                transaction.query().delete(
                    match(
                        `var`(O).isa(`object`.type)
                            .has(`object`.idType, `object`.idValue),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        `var`(A).rel(ACCESSED_OBJECT, O).isa(ACCESS),
                        `var`(P).rel(PERMITTED_ACCESS, A).isa(PERMISSION)
                    ).delete(
                        `var`(P).isa(PERMISSION)
                    )
                )

                transaction.query().delete(
                    match(
                        `var`(O).isa(`object`.type)
                            .has(`object`.idType, `object`.idValue),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        `var`(A).rel(ACCESSED_OBJECT, O).isa(ACCESS),
                        `var`(R).rel(REQUESTED_CHANGE, A).isa(CHANGE_REQUEST)
                    ).delete(
                        `var`(R).isa(CHANGE_REQUEST)
                    )
                )

                transaction.query().delete(
                    match(
                        `var`(O).isa(`object`.type)
                            .has(`object`.idType, `object`.idValue),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        `var`(A).rel(ACCESSED_OBJECT, O).isa(ACCESS)
                    ).delete(
                        `var`(A).isa(ACCESS)
                    )
                )

                transaction.query().delete(
                    match(
                        `var`(O).isa(`object`.type)
                            .has(`object`.idType, `object`.idValue),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        `var`(OW).rel(OWNED_OBJECT, O).isa(OBJECT_OWNERSHIP)
                    ).delete(
                        `var`(OW).isa(OBJECT_OWNERSHIP)
                    )
                )

                transaction.query().delete(
                    match(
                        `var`(O).isa(`object`.type)
                            .has(`object`.idType, `object`.idValue),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        `var`(ME).rel(O).isa(COLLECTION_MEMBERSHIP)
                    ).delete(
                        `var`(ME).isa(COLLECTION_MEMBERSHIP)
                    )
                )

                transaction.query().delete(
                    match(
                        `var`(O).isa(`object`.type)
                            .has(`object`.idType, `object`.idValue),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        `var`(ME).rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP)
                    ).delete(
                        `var`(O).isa(`object`.type),
                        `var`(ME).isa(COMPANY_MEMBERSHIP)
                    )
                )

                transaction.commit()
            }
        }

        return listOf<Report>()
    }

    override fun attemptAccess(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val subject = getRandomEntity(session, company, randomSource, SUBJECT).asSubject()
        val candidateObjects: List<TypeDBObject>

        session.transaction(READ, options).use { transaction ->
            candidateObjects = transaction.query().match(
                match(
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(ID, `var`(O_ID)),
                    rel(ACCESSED_OBJECT, O).isa(ACCESS),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE)
                )
            ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
        }

        val `object` = randomSource.choose(candidateObjects)
        val candidateOperations: List<TypeDBAction>

        session.transaction(READ, options).use { transaction ->
            candidateOperations = transaction.query().match(
                match(
                    `var`(A).isa(OPERATION)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(ACTION_NAME, `var`(A_NAME)),
                    `var`(O).isa(`object`.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(`object`.idType, `object`.idValue),
                    rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS)
                )
            ).toList().map { TypeDBAction(OPERATION, stringValue(it[A_NAME])) }
        }

        val action = randomSource.choose(candidateOperations)
        val matches: List<ConceptMap>

        session.transaction(READ, options).use { transaction ->
            matches = transaction.query().match(
                match(
                    `var`(S).isa(subject.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(subject.idType, subject.idValue),
                    `var`(O).isa(`object`.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(`object`.idType, `object`.idValue),
                    `var`(A).isa(action.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(action.idType, action.idValue),
                    `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION)
                        .has(VALIDITY, true)
                )
            ).toList()
        }

        val result = matches.isNotEmpty()
        return listOf<Report>()
    }

    override fun submitChangeRequest(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val requestingSubject = getRandomEntity(session, company, randomSource, SUBJECT).asSubject()
        val requestedSubject = getRandomEntity(session, company, randomSource, SUBJECT).asSubject()
        val candidateObjects: List<TypeDBObject>

        session.transaction(READ, options).use { transaction ->
            candidateObjects = transaction.query().match(
                match(
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(ID, `var`(O_ID)),
                    rel(ACCESSED_OBJECT, O).isa(ACCESS),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE)
                )
            ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
        }

        val `object` = randomSource.choose(candidateObjects)
        val candidateOperations: List<TypeDBAction>

        session.transaction(READ, options).use { transaction ->
            candidateOperations = transaction.query().match(
                match(
                    `var`(A).isa(OPERATION)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(ACTION_NAME, `var`(A_NAME)),
                    `var`(O).isa(`object`.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(`object`.idType, `object`.idValue),
                    rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS)
                )
            ).toList().map { TypeDBAction(OPERATION, stringValue(it[A_NAME])) }
        }

        val action = randomSource.choose(candidateOperations)

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(S_REQUESTING).isa(requestingSubject.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(requestingSubject.idType, requestingSubject.idValue),
                    `var`(S_REQUESTED).isa(requestedSubject.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(requestedSubject.idType, requestedSubject.idValue),
                    `var`(O).isa(`object`.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(`object`.idType, `object`.idValue),
                    `var`(A).isa(action.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(action.idType, action.idValue),
                    `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A)
                ).insert(
                    rel(REQUESTING_SUBJECT, S_REQUESTING).rel(REQUESTED_SUBJECT, S_REQUESTED).rel(REQUESTED_CHANGE, AC).isa(CHANGE_REQUEST)
                )
            )

            transaction.commit()
        }

        return listOf<Report>()
    }

    private fun createObject(session: TypeDBSession, company: Company, randomSource: RandomSource, objectType: TypeDBObjectType) {
        val parentType = when (objectType) {
            TypeDBObjectType.FILE -> TypeDBObjectType.DIRECTORY
            TypeDBObjectType.DIRECTORY -> TypeDBObjectType.DIRECTORY
            TypeDBObjectType.INTERFACE -> TypeDBObjectType.APPLICATION
            TypeDBObjectType.APPLICATION -> throw IllegalArgumentException()
            TypeDBObjectType.RECORD -> TypeDBObjectType.TABLE
            TypeDBObjectType.TABLE -> TypeDBObjectType.DATABASE
            TypeDBObjectType.DATABASE -> throw IllegalArgumentException()
        }

        val parent = getRandomEntity(session, company, randomSource, parentType.label).asObject()

        val `object` = when (objectType) {
            TypeDBObjectType.FILE -> TypeDBFile.initialise(parent.idValue, context.seedData, randomSource).asObject()
            TypeDBObjectType.DIRECTORY -> TypeDBDirectory.initialise(parent.idValue, context.seedData, randomSource).asObject()
            TypeDBObjectType.INTERFACE -> TypeDBInterface.initialise(parent.idValue, context.seedData, randomSource).asObject()
            TypeDBObjectType.APPLICATION -> throw IllegalArgumentException()
            TypeDBObjectType.RECORD -> TypeDBRecord.initialise(randomSource).asObject()
            TypeDBObjectType.TABLE -> TypeDBTable.initialise(parent.idValue, context.seedData, randomSource).asObject()
            TypeDBObjectType.DATABASE -> throw IllegalArgumentException()
        }

        val owner = getRandomEntity(session, company, randomSource, SUBJECT).asSubject()
        val validActions: List<TypeDBAction>

        session.transaction(READ, options).use { transaction ->
            validActions = transaction.query().match(
                match(
                    `var`(A).isa(ACTION)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(OBJECT_TYPE, `object`.type)
                        .has(ACTION_NAME, `var`(A_NAME)),
                    `var`(A).isaX(A_TYPE)
                )
            ).toList().map { TypeDBAction(it[A_TYPE], it[A_NAME]) }
        }

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    `var`(CO).isa(parent.type)
                        .has(parent.idType, parent.idValue),
                    `var`(S).isa(owner.type)
                        .has(owner.idType, owner.idValue),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, CO).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP)
                ).insert(
                    `var`(O).isa(`object`.type)
                        .has(`object`.idType, `object`.idValue),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COLLECTION, CO).rel(COLLECTION_MEMBER, O).isa(COLLECTION_MEMBERSHIP),
                    rel(OWNED_OBJECT, O).rel(OBJECT_OWNER, S).isa(OBJECT_OWNERSHIP)
                )
            )

            validActions.parallelStream().forEach { action ->
                transaction.query().insert(
                    match(
                        `var`(O).isa(`object`.type)
                            .has(`object`.idType, `object`.idValue),
                        `var`(A).isa(action.type)
                            .has(action.idType, action.idValue),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A).isa(COMPANY_MEMBERSHIP)
                    ).insert(
                        rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS)
                    )
                )
            }

            transaction.commit()
        }
    }

    private fun createDatabase(session: TypeDBSession, company: Company, randomSource: RandomSource) {
        val database = TypeDBDatabase.initialise(context.seedData, randomSource)
        val owner = getRandomEntity(session, company, randomSource, SUBJECT).asSubject()
        val validActions: List<TypeDBAction>

        session.transaction(READ, options).use { transaction ->
            validActions = transaction.query().match(
                match(
                    `var`(A).isa(ACTION)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(OBJECT_TYPE, DATABASE)
                        .has(ACTION_NAME, `var`(A_NAME)),
                    `var`(A).isaX(A_TYPE)
                )
            ).toList().map { TypeDBAction(it[A_TYPE], it[A_NAME]) }
        }

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    `var`(S).isa(owner.type)
                        .has(owner.idType, owner.idValue),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP)
                ).insert(
                    `var`(O).isa(DATABASE)
                        .has(NAME, database.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    rel(OWNED_OBJECT, O).rel(OBJECT_OWNER, S).isa(OBJECT_OWNERSHIP)
                )
            )

            validActions.parallelStream().forEach { action ->
                transaction.query().insert(
                    match(
                        `var`(O).isa(DATABASE)
                            .has(NAME, database.name),
                        `var`(A).isa(action.type)
                            .has(action.idType, action.idValue),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A).isa(COMPANY_MEMBERSHIP)
                    ).insert(
                        rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS)
                    )
                )
            }

            transaction.commit()
        }
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
