package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.agent.UserAgent
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
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY_NAME
import com.vaticle.typedb.iam.simulation.typedb.Util.cvar
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

class TypeDBUserAgent(client: TypeDBClient, context:Context): UserAgent<TypeDBSession>(client, context) {
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

        session.transaction(READ, options).use { tx ->
            candidateObjects = tx.query().match(
                match(
                    cvar(O).isaX(cvar(O_TYPE))
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(ROOT_COLLECTION, false)
                        .has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                )
            ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
        }

        if (candidateObjects.isEmpty()) return listOf<Report>()
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
                    rel(PARENT_COLLECTION, O).rel(COLLECTION_MEMBER, O_MEMBER).isa(COLLECTION_MEMBERSHIP),
                )
            ).toList().map { TypeDBObject(it[O_MEMBER_TYPE], it[O_MEMBER_ID_TYPE], it[O_MEMBER_ID]) }
        }

        val objectsToDelete = members + listOf(obj)

        session.transaction(WRITE, options).use { tx ->
            objectsToDelete.parallelStream().forEach { obj ->
                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(A).rel(ACCESSED_OBJECT, O).isa(ACCESS),
                        cvar(P).rel(PERMITTED_ACCESS, A).isa(PERMISSION),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(P).isa(PERMISSION)
                    )
                )

                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(A).rel(ACCESSED_OBJECT, O).isa(ACCESS),
                        cvar(R).rel(REQUESTED_CHANGE, A).isa(CHANGE_REQUEST),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(R).isa(CHANGE_REQUEST),
                    )
                )

                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(A).rel(ACCESSED_OBJECT, O).isa(ACCESS),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(A).isa(ACCESS),
                    )
                )

                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(OW).rel(OWNED_OBJECT, O).isa(OBJECT_OWNERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(OW).isa(OBJECT_OWNERSHIP),
                    )
                )

                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(ME).rel(O).isa(COLLECTION_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(ME).isa(COLLECTION_MEMBERSHIP),
                    )
                )

                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(ME).rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(O).isa(obj.type),
                        cvar(ME).isa(COMPANY_MEMBERSHIP),
                    )
                )
            }

            tx.commit()
        }

        return listOf<Report>()
    }

    override fun attemptAccess(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val subject = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf<Report>()
        val candidateObjects: List<TypeDBObject>

        session.transaction(READ, options).use { tx ->
            candidateObjects = tx.query().match(
                match(
                    cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    rel(ACCESSED_OBJECT, O).isa(ACCESS),
                )
            ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
        }

        if (candidateObjects.isEmpty()) return listOf<Report>()
        val obj = randomSource.choose(candidateObjects)
        val candidateOperations: List<TypeDBAction>

        session.transaction(READ, options).use { tx ->
            candidateOperations = tx.query().match(
                match(
                    cvar(A).isa(OPERATION).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A_NAME)),
                    cvar(O).isa(obj.type).has(PARENT_COMPANY_NAME, company.name).has(obj.idType, obj.idValue),
                    rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
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
                    cvar(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION).has(VALIDITY, true)
                )
            ).toList()
        }

        val result = matches.isNotEmpty()
        return listOf<Report>()
    }

    override fun submitChangeRequest(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val requestingSubject = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf<Report>()
        val requestedSubject = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf<Report>()
        val candidateObjects: List<TypeDBObject>

        session.transaction(READ, options).use { tx ->
            candidateObjects = tx.query().match(
                match(
                    cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    rel(ACCESSED_OBJECT, O).isa(ACCESS),
                )
            ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
        }

        if (candidateObjects.isEmpty()) return listOf<Report>()
        val obj = randomSource.choose(candidateObjects)
        val candidateOperations: List<TypeDBAction>

        session.transaction(READ, options).use { tx ->
            candidateOperations = tx.query().match(
                match(
                    cvar(A).isa(OPERATION).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A_NAME)),
                    cvar(O).isa(obj.type).has(PARENT_COMPANY_NAME, company.name).has(obj.idType, obj.idValue),
                    rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                )
            ).toList().map { TypeDBAction(OPERATION, stringValue(it[A_NAME])) }
        }

        val action = randomSource.choose(candidateOperations)

        session.transaction(WRITE, options).use { tx ->
            tx.query().insert(
                match(
                    cvar(S_REQUESTING).isa(requestingSubject.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(requestingSubject.idType, requestingSubject.idValue),
                    cvar(S_REQUESTED).isa(requestedSubject.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(requestedSubject.idType, requestedSubject.idValue),
                    cvar(O).isa(obj.type).has(PARENT_COMPANY_NAME, company.name).has(obj.idType, obj.idValue),
                    cvar(A).isa(action.type).has(PARENT_COMPANY_NAME, company.name).has(action.idType, action.idValue),
                    cvar(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A),
                ).insert(
                    rel(REQUESTING_SUBJECT, S_REQUESTING).rel(REQUESTED_SUBJECT, S_REQUESTED).rel(REQUESTED_CHANGE, AC).isa(CHANGE_REQUEST)
                )
            )

            tx.commit()
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

        val parent = getRandomEntity(session, company, randomSource, parentType.label)?.asObject() ?: return

        val obj = when (objectType) {
            TypeDBObjectType.FILE -> TypeDBFile.initialise(parent.idValue, context.seedData, randomSource)
            TypeDBObjectType.DIRECTORY -> TypeDBDirectory.initialise(parent.idValue, context.seedData, randomSource)
            TypeDBObjectType.INTERFACE -> TypeDBInterface.initialise(parent.idValue, context.seedData, randomSource)
            TypeDBObjectType.APPLICATION -> throw IllegalArgumentException()
            TypeDBObjectType.RECORD -> TypeDBRecord.initialise(randomSource)
            TypeDBObjectType.TABLE -> TypeDBTable.initialise(parent.idValue, context.seedData, randomSource)
            TypeDBObjectType.DATABASE -> throw IllegalArgumentException()
        }

        val owner = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return
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

        session.transaction(WRITE, options).use { tx ->
            tx.query().insert(
                match(
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(CO).isa(parent.type).has(parent.idType, parent.idValue),
                    cvar(S).isa(owner.type).has(owner.idType, owner.idValue),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, CO).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COLLECTION, CO).rel(COLLECTION_MEMBER, O).isa(COLLECTION_MEMBERSHIP),
                    rel(OWNED_OBJECT, O).rel(OBJECT_OWNER, S).isa(OBJECT_OWNERSHIP),
                )
            )

            validActions.parallelStream().forEach { action ->
                tx.query().insert(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(A).isa(action.type).has(action.idType, action.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A).isa(COMPANY_MEMBERSHIP),
                    ).insert(
                        rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    )
                )
            }

            tx.commit()
        }
    }

    private fun createDatabase(session: TypeDBSession, company: Company, randomSource: RandomSource) {
        val database = TypeDBDatabase.initialise(context.seedData, randomSource)
        val owner = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return
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

        session.transaction(WRITE, options).use { tx ->
            tx.query().insert(
                match(
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(S).isa(owner.type).has(owner.idType, owner.idValue),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    cvar(O).isa(DATABASE).has(NAME, database.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    rel(OWNED_OBJECT, O).rel(OBJECT_OWNER, S).isa(OBJECT_OWNERSHIP),
                )
            )

            validActions.parallelStream().forEach { action ->
                tx.query().insert(
                    match(
                        cvar(O).isa(DATABASE).has(NAME, database.name),
                        cvar(A).isa(action.type).has(action.idType, action.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A).isa(COMPANY_MEMBERSHIP),
                    ).insert(
                        rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    )
                )
            }

            tx.commit()
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
