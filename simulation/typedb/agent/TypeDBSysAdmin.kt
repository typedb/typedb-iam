package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.agent.SysAdmin
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.concept.*
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACCESS
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACCESSED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.ATTRIBUTE
import com.vaticle.typedb.iam.simulation.typedb.Labels.CHANGE_REQUEST
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.EMAIL
import com.vaticle.typedb.iam.simulation.typedb.Labels.ENTITY
import com.vaticle.typedb.iam.simulation.typedb.Labels.FULL_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.ID
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_GROUP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_GROUP
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMISSION
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMITTED_ACCESS
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMITTED_SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERSON
import com.vaticle.typedb.iam.simulation.typedb.Labels.REQUESTED_CHANGE
import com.vaticle.typedb.iam.simulation.typedb.Labels.REQUESTED_SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.REQUESTING_SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.REVIEW_DATE
import com.vaticle.typedb.iam.simulation.typedb.Labels.SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_ACCOUNT
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_GROUP
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALIDITY
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALID_ACTION
import com.vaticle.typedb.iam.simulation.typedb.Util.getRandomEntity
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import java.lang.IllegalArgumentException
import kotlin.streams.toList

class TypeDBSysAdmin(client: TypeDBClient, context:Context): SysAdmin<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions().infer(true)

    override fun addUser(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val user = Person.initialise(company, context.seedData, randomSource)

        session.transaction(WRITE).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                ).insert(
                    `var`(S).isa(PERSON)
                        .has(FULL_NAME, user.name)
                        .has(EMAIL, user.email),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP)
                )
            )

            transaction.commit()
        }

        return listOf<Report>()
    }

    override fun removeUser(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val userType = randomSource.choose(context.seedData.subjectTypes.filter { it.type == USER })
        deleteSubject(session, company, randomSource, userType)
        return listOf<Report>()
    }

    override fun createUserGroup(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val groupType = randomSource.choose(context.seedData.subjectTypes.filter { it.type == USER_GROUP && it.generable })

        val group = when (groupType) {
            SubjectType.PERSON -> throw IllegalArgumentException()
            SubjectType.BUSINESS_UNIT -> throw IllegalArgumentException()
            SubjectType.USER_ROLE -> throw IllegalArgumentException()
            SubjectType.USER_ACCOUNT -> UserAccount.initialise(company, context.seedData, randomSource).asSubject()
        }

        val owner = getRandomEntity(session, company, randomSource, SUBJECT).asSubject()

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    `var`(S_OWNER).isa(owner.type)
                        .has(owner.idType, owner.idValue),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S_OWNER).isa(COMPANY_MEMBERSHIP)
                ).insert(
                    `var`(S).isa(group.type)
                        .has(group.idType, group.idValue),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    rel(OWNED_GROUP, S).rel(GROUP_OWNER, S_OWNER).isa(GROUP_OWNERSHIP)
                )
            )

            transaction.commit()
        }

        return listOf<Report>()
    }

    override fun deleteUserGroup(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val groupType = SubjectType.USER_ACCOUNT
        deleteSubject(session, company, randomSource, groupType)
        return listOf<Report>()
    }

    override fun listSubjectGroupMemberships(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val subject = getRandomEntity(session, company, randomSource, SUBJECT).asSubject()
        val groups: List<Subject>

        session.transaction(READ, options).use { transaction ->
            groups = transaction.query().match(
                match(
                    `var`(S_MEMBER).isa(subject.type)
                        .has(PARENT_COMPANY, company.name)
                        .has(subject.idType, subject.idValue),
                    `var`(S).isa(USER_GROUP)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, S_ID),
                    rel(PARENT_GROUP, S).rel(GROUP_MEMBER, S_MEMBER).isa(GROUP_MEMBERSHIP),
                    `var`(S).isaX(S_TYPE),
                    `var`(S_ID).isaX(S_ID_TYPE)
                )
            ).toList().map { Subject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]) }
        }

        return listOf<Report>()
    }

    override fun listSubjectPermissions(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val subject = getRandomEntity(session, company, randomSource, SUBJECT).asSubject()
        val permissions: List<Permission>

        session.transaction(READ, options).use { transaction ->
            permissions = transaction.query().match(
                match(
                    `var`(S).isa(subject.type)
                        .has(PARENT_COMPANY, company.name)
                        .has(subject.idType, subject.idValue),
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, O_ID),
                    `var`(A).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(ACTION_NAME, A_NAME),
                    `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    `var`(P).rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION)
                        .has(VALIDITY, P_VALIDITY)
                        .has(REVIEW_DATE, P_DATE),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE),
                    `var`(A).isaX(A_TYPE)
                )
            ).toList().map {
                Permission(
                    subject,
                    Access(
                        Object(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                        Action(it[A_TYPE], it[A_NAME])
                    ),
                    it[P_VALIDITY],
                    it[P_DATE]
                )
            }
        }

        return listOf<Report>()
    }

    override fun listObjectPermissionHolders(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val `object` = getRandomEntity(session, company, randomSource, OBJECT).asObject()
        val permissions: List<Permission>

        session.transaction(READ, options).use { transaction ->
            permissions = transaction.query().match(
                match(
                    `var`(O).isa(`object`.type)
                        .has(PARENT_COMPANY, company.name)
                        .has(`object`.idType, `object`.idValue),
                    `var`(S).isa(SUBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, S_ID),
                    `var`(A).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(ACTION_NAME, A_NAME),
                    `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    `var`(P).rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION)
                        .has(VALIDITY, P_VALIDITY)
                        .has(REVIEW_DATE, P_DATE),
                    `var`(S).isaX(S_TYPE),
                    `var`(S_ID).isaX(S_ID_TYPE),
                    `var`(A).isaX(A_TYPE)
                )
            ).toList().map {
                Permission(
                    Subject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    Access(
                        `object`,
                        Action(it[A_TYPE], it[A_NAME])
                    ),
                    it[P_VALIDITY],
                    it[P_DATE]
                )
            }
        }

        return listOf<Report>()
    }

    override fun reviewChangeRequests(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val requests: List<ChangeRequest>

        session.transaction(READ, options).use { transaction ->
            requests = transaction.query().match(
                match(
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, O_ID),
                    `var`(A).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(ACTION_NAME, A_NAME),
                    `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    `var`(S_REQUESTING).isa(SUBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, S_REQUESTING_ID),
                    `var`(S_REQUESTED).isa(SUBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, S_REQUESTED_ID),
                    rel(REQUESTING_SUBJECT, S_REQUESTING).rel(REQUESTED_SUBJECT, S_REQUESTED).rel(REQUESTED_CHANGE, AC).isa(CHANGE_REQUEST),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE),
                    `var`(A).isaX(A_TYPE),
                    `var`(S_REQUESTING).isaX(S_REQUESTING_TYPE),
                    `var`(S_REQUESTING_ID).isaX(S_REQUESTING_ID_TYPE),
                    `var`(S_REQUESTED).isaX(S_REQUESTED_TYPE),
                    `var`(S_REQUESTED_ID).isaX(S_REQUESTED_ID_TYPE)
                )
            ).toList().map {
                ChangeRequest(
                    Subject(it[S_REQUESTING_TYPE], it[S_REQUESTING_ID_TYPE], it[S_REQUESTING_ID]),
                    Subject(it[S_REQUESTED_TYPE], it[S_REQUESTED_ID_TYPE], it[S_REQUESTED_ID]),
                    Access(
                        Object(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                        Action(it[A_TYPE], it[A_NAME])
                    )
                )
            }
        }

        requests.forEach { request ->
            val requestingSubject = request.requestingSubject
            val requestedSubject = request.requestedSubject
            val accessedObject = request.requestedAccess.accessedObject
            val validAction = request.requestedAccess.validAction

            session.transaction(WRITE, options).use { transaction ->
                transaction.query().delete(
                    match(
                        `var`(O).isa(accessedObject.type)
                            .has(accessedObject.idType, accessedObject.idValue),
                        `var`(A).isa(validAction.type)
                            .has(validAction.idType, validAction.idValue),
                        `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                        `var`(S_REQUESTING).isa(requestingSubject.type)
                            .has(requestingSubject.idType, requestingSubject.idValue),
                        `var`(S_REQUESTED).isa(requestedSubject.type)
                            .has(requestedSubject.idType, requestedSubject.idValue),
                        `var`(R).rel(REQUESTING_SUBJECT, S_REQUESTING).rel(REQUESTED_SUBJECT, S_REQUESTED).rel(REQUESTED_CHANGE, AC).isa(CHANGE_REQUEST),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S_REQUESTING).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S_REQUESTED).isa(COMPANY_MEMBERSHIP)
                    ).delete(
                        `var`(R).isa(CHANGE_REQUEST)
                    )
                )

                if (randomSource.nextInt(100) < context.model.requestApprovalPercentage) {
                    transaction.query().insert(
                        match(
                            `var`(S).isa(requestedSubject.type)
                                .has(requestedSubject.idType, requestedSubject.idValue),
                            `var`(O).isa(accessedObject.type)
                                .has(accessedObject.idType, accessedObject.idValue),
                            `var`(A).isa(validAction.type)
                                .has(validAction.idType, validAction.idValue),
                            `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                            `var`(C).isa(COMPANY)
                                .has(NAME, company.name),
                            rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                            rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                            rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A).isa(COMPANY_MEMBERSHIP)
                        ).insert(
                            rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION)
                                .has(REVIEW_DATE, (context.iterationNumber + context.model.permissionReviewAge).toLong())
                        )
                    )
                }

                transaction.commit()
            }
        }

        return listOf<Report>()
    }

    override fun collectGarbage(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        session.transaction(WRITE, options).use { transaction ->
            transaction.query().delete(
                match(
                    `var`(AT).isa(ATTRIBUTE),
                    not(
                        `var`().has(ATTRIBUTE, AT)
                    ),
                    not(
                        `var`(AT).isa(PARENT_COMPANY)
                    )
                ).delete(
                    `var`(AT).isa(ATTRIBUTE)
                )
            )
        }

        return listOf<Report>()
    }

    private fun deleteSubject(session: TypeDBSession, company: Company, randomSource: RandomSource, subjectType: SubjectType) {
        val subject = getRandomEntity(session, company, randomSource, subjectType.label).asSubject()
        val newOwner = getRandomEntity(session, company, randomSource, subjectType.label).asSubject()
        val ownedEntities: List<Entity>

        session.transaction(READ, options).use { transaction ->
            ownedEntities = transaction.query().match(
                match(
                    `var`(E).isa(ENTITY)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, E_ID),
                    `var`(S).isa(subject.type)
                        .has(PARENT_COMPANY, company.name)
                        .has(subject.idType, subject.idValue),
                    rel(OWNED, E).rel(OWNER, S).isa(OWNERSHIP),
                    `var`(E).isaX(E_TYPE),
                    `var`(E_ID).isaX(E_ID_TYPE)
                )
            ).toList().map { Entity(it[E_TYPE], it[E_ID_TYPE], it[E_ID]) }
        }

        session.transaction(WRITE, options).use { transaction ->
            ownedEntities.parallelStream().forEach { entity ->
                transaction.query().delete(
                    match(
                        `var`(E).isa(entity.type)
                            .has(entity.idType, entity.idValue),
                        `var`(S).isa(subject.type)
                            .has(subject.idType, subject.idValue),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, E).isa(COMPANY_MEMBERSHIP,),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP,),
                        `var`(OW).rel(OWNED, E).rel(OWNER, S).isa(OWNERSHIP)
                    ).delete(
                        `var`(OW).isa(OWNERSHIP)
                    )
                )

                transaction.query().insert(
                    match(
                        `var`(E).isa(entity.type)
                            .has(entity.idType, entity.idValue),
                        `var`(S).isa(newOwner.type)
                            .has(newOwner.idType, newOwner.idValue),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, E).isa(COMPANY_MEMBERSHIP,),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP,)
                    ).insert(
                        rel(OWNED, E).rel(OWNER, S).isa(OWNERSHIP)
                    )
                )
            }

            transaction.query().delete(
                match(
                    `var`(S).isa(subject.type)
                        .has(subject.idType, subject.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    `var`(ME).rel(S).isa(GROUP_MEMBERSHIP)
                ).delete(
                    `var`(ME).isa(GROUP_MEMBERSHIP)
                )
            )

            transaction.query().delete(
                match(
                    `var`(S).isa(subject.type)
                        .has(subject.idType, subject.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    `var`(P).rel(PERMITTED_SUBJECT, S).isa(PERMISSION)
                ).delete(
                    `var`(P).isa(PERMISSION)
                )
            )

            transaction.query().delete(
                match(
                    `var`(S).isa(subject.type)
                        .has(subject.idType, subject.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    `var`(R).rel(S).isa(CHANGE_REQUEST)
                ).delete(
                    `var`(R).isa(CHANGE_REQUEST)
                )
            )

            transaction.query().delete(
                match(
                    `var`(S).isa(subject.type)
                        .has(subject.idType, subject.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    `var`(ME).rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP)
                ).delete(
                    `var`(S).isa(subject.type),
                    `var`(ME).isa(COMPANY_MEMBERSHIP)
                )
            )

            transaction.commit()
        }
    }

    companion object {
        private const val A = "a"
        private const val AC = "ac"
        private const val AT = "at"
        private const val A_NAME = "a-name"
        private const val A_TYPE = "a-type"
        private const val C = "c"
        private const val E = "e"
        private const val E_ID = "e-id"
        private const val E_ID_TYPE = "e-id-type"
        private const val E_TYPE = "e-type"
        private const val ME = "me"
        private const val O = "o"
        private const val OW = "ow"
        private const val O_ID = "o-id"
        private const val O_ID_TYPE = "o-id-type"
        private const val O_TYPE = "o-type"
        private const val P = "p"
        private const val P_DATE = "p-date"
        private const val P_VALIDITY = "p-validity"
        private const val R = "r"
        private const val S = "s"
        private const val S_ID = "s-id"
        private const val S_ID_TYPE = "s-id-type"
        private const val S_MEMBER = "s-member"
        private const val S_OWNER = "s-owner"
        private const val S_REQUESTED = "s-requested"
        private const val S_REQUESTED_ID = "s-requested-id"
        private const val S_REQUESTED_ID_TYPE = "s-requested-id-type"
        private const val S_REQUESTED_TYPE = "s-requested-type"
        private const val S_REQUESTING = "s-requesting"
        private const val S_REQUESTING_ID = "s-requesting-id"
        private const val S_REQUESTING_ID_TYPE = "s-requesting-id-type"
        private const val S_REQUESTING_TYPE = "s-requesting-type"
        private const val S_TYPE = "s-type"
    }
}
