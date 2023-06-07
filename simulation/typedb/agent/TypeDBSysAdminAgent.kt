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
import com.vaticle.typedb.iam.simulation.agent.SysAdminAgent
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.Util.iterationDate
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
import com.vaticle.typedb.iam.simulation.typedb.Labels.FULL_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.ID
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_GROUP
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
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_GROUP
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALIDITY
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALID_ACTION
import com.vaticle.typedb.iam.simulation.typedb.Util.getRandomEntity
import com.vaticle.typedb.iam.simulation.typedb.concept.*
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.common.concept.Person
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY_NAME
import com.vaticle.typedb.iam.simulation.typedb.Util.cvar
import com.vaticle.typedb.benchmark.framework.common.seed.RandomSource
import com.vaticle.typedb.benchmark.framework.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import java.lang.IllegalArgumentException
import kotlin.streams.toList

class TypeDBSysAdminAgent(client: TypeDBClient, context:Context): SysAdminAgent<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions.core().infer(true)

    override fun addUser(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val user = Person.initialise(company, context.seedData, randomSource)
        
        session.transaction(READ, options).use { tx ->
            if (
                tx.query().match(
                    match(
                        cvar(S).isa(PERSON).has(EMAIL, user.email).has(PARENT_COMPANY_NAME, company.name)
                    )
                ).toList().isNotEmpty()
            ) return listOf()
        }

        session.transaction(WRITE).use { tx ->
            tx.query().insert(
                match(
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                ).insert(
                    cvar(S).isa(PERSON).has(FULL_NAME, user.name).has(EMAIL, user.email),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                )
            )

            tx.commit()
        }

        return listOf()
    }

    override fun removeUser(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val userType = randomSource.choose(TypeDBSubjectType.values().asList().filter { it.type == USER })
        deleteSubject(session, company, randomSource, userType)
        return listOf()
    }

    override fun createUserGroup(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val groupType = randomSource.choose(TypeDBSubjectType.values().asList().filter { it.type == USER_GROUP && it.generable })

        val group = when (groupType) {
            TypeDBSubjectType.PERSON -> throw IllegalArgumentException()
            TypeDBSubjectType.BUSINESS_UNIT -> throw IllegalArgumentException()
            TypeDBSubjectType.USER_ROLE -> throw IllegalArgumentException()
            TypeDBSubjectType.USER_ACCOUNT -> TypeDBUserAccount.initialise(company, context.seedData, randomSource)
        }

        session.transaction(READ, options).use { tx ->
            if (
                tx.query().match(
                    match(
                        cvar(S).isa(group.type).has(group.idType, group.idValue).has(PARENT_COMPANY_NAME, company.name)
                    )
                ).toList().isNotEmpty()
            ) return listOf()
        }

        val owner = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf()

        session.transaction(WRITE).use { tx ->
            tx.query().insert(
                match(
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(S_OWNER).isa(owner.type).has(owner.idType, owner.idValue),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S_OWNER)).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    cvar(S).isa(group.type).has(group.idType, group.idValue),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                    rel(OWNED_GROUP, cvar(S)).rel(GROUP_OWNER, cvar(S_OWNER)).isa(GROUP_OWNERSHIP),
                )
            )

            tx.commit()
        }

        return listOf()
    }

    override fun deleteUserGroup(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val groupType = TypeDBSubjectType.USER_ACCOUNT
        deleteSubject(session, company, randomSource, groupType)
        return listOf()
    }

    override fun listSubjectGroupMemberships(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val subject = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf()
        val groups: List<TypeDBSubject>

        session.transaction(READ, options).use { tx ->
            groups = tx.query().match(
                match(
                    cvar(S_MEMBER).isa(subject.type).has(PARENT_COMPANY_NAME, company.name).has(subject.idType, subject.idValue),
                    cvar(S).isaX(cvar(S_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(S_ID)),
                    cvar(S_ID).isaX(cvar(S_ID_TYPE)),
                    cvar(S_TYPE).sub(USER_GROUP),
                    cvar(S_ID_TYPE).sub(ID),
                    rel(PARENT_GROUP, cvar(S)).rel(GROUP_MEMBER, cvar(S_MEMBER)).isa(GROUP_MEMBERSHIP),
                )
            ).toList().map {
                TypeDBSubject(it[S_TYPE], it[S_ID_TYPE], it[S_ID])
            }
        }

        return listOf()
    }

    override fun listSubjectPermissions(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val subject = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf()
        val permissions: List<TypeDBPermission>

        session.transaction(READ, options).use { tx ->
            permissions = tx.query().match(
                match(
                    cvar(S).isa(subject.type).has(PARENT_COMPANY_NAME, company.name).has(subject.idType, subject.idValue),
                    cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(A).isaX(cvar(A_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A_NAME)),
                    cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                    cvar(P).rel(PERMITTED_SUBJECT, cvar(S)).rel(PERMITTED_ACCESS, cvar(AC)).isa(PERMISSION)
                        .has(VALIDITY, cvar(P_VALIDITY))
                        .has(REVIEW_DATE, cvar(P_DATE)),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    cvar(A_TYPE).sub(ACTION),
                )
            ).toList().map {
                TypeDBPermission(
                    subject,
                    TypeDBAccess(
                        TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                        TypeDBAction(it[A_TYPE], it[A_NAME])
                    ),
                    it[P_VALIDITY],
                    it[P_DATE]
                )
            }
        }

        return listOf()
    }

    override fun listObjectPermissionHolders(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val obj = getRandomEntity(session, company, randomSource, OBJECT)?.asObject() ?: return listOf()
        val permissions: List<TypeDBPermission>

        session.transaction(READ, options).use { tx ->
            permissions = tx.query().match(
                match(
                    cvar(O).isa(obj.type).has(PARENT_COMPANY_NAME, company.name).has(obj.idType, obj.idValue),
                    cvar(S).isaX(cvar(S_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(S_ID)),
                    cvar(S_ID).isaX(cvar(S_ID_TYPE)),
                    cvar(A).isaX(cvar(A_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A_NAME)),
                    cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                    cvar(P).rel(PERMITTED_SUBJECT, cvar(S)).rel(PERMITTED_ACCESS, cvar(AC)).isa(PERMISSION)
                        .has(VALIDITY, cvar(P_VALIDITY))
                        .has(REVIEW_DATE, cvar(P_DATE)),
                    cvar(S_TYPE).sub(SUBJECT),
                    cvar(S_ID_TYPE).sub(ID),
                    cvar(A_TYPE).sub(ACTION),
                )
            ).toList().map {
                TypeDBPermission(
                    TypeDBSubject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    TypeDBAccess(
                        obj,
                        TypeDBAction(it[A_TYPE], it[A_NAME])
                    ),
                    it[P_VALIDITY],
                    it[P_DATE]
                )
            }
        }

        return listOf()
    }

    override fun reviewChangeRequests(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        while (true) {
            val requests: List<TypeDBChangeRequest>

            session.transaction(READ, options).use { tx ->
                requests = tx.query().match(
                    match(
                        cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                        cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                        cvar(A).isa(cvar(A_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A_NAME)),
                        cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                        cvar(S_REQUESTING).isaX(cvar(S_REQUESTING_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(S_REQUESTING_ID)),
                        cvar(S_REQUESTING_ID).isaX(cvar(S_REQUESTING_ID_TYPE)),
                        cvar(S_REQUESTED).isaX(cvar(S_REQUESTED_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(S_REQUESTED_ID)),
                        cvar(S_REQUESTED_ID).isaX(cvar(S_REQUESTED_ID_TYPE)),
                        cvar(O_TYPE).sub(OBJECT),
                        cvar(O_ID_TYPE).sub(ID),
                        cvar(A_TYPE).sub(ACTION),
                        cvar(S_REQUESTING_TYPE).sub(SUBJECT),
                        cvar(S_REQUESTING_ID_TYPE).sub(ID),
                        cvar(S_REQUESTED_TYPE).sub(SUBJECT),
                        cvar(S_REQUESTED_ID_TYPE).sub(ID),
                        rel(REQUESTING_SUBJECT, cvar(S_REQUESTING)).rel(REQUESTED_SUBJECT, cvar(S_REQUESTED)).rel(REQUESTED_CHANGE, cvar(AC)).isa(CHANGE_REQUEST),
                    ).limit(1)
                ).toList().map {
                    TypeDBChangeRequest(
                        TypeDBSubject(it[S_REQUESTING_TYPE], it[S_REQUESTING_ID_TYPE], it[S_REQUESTING_ID]),
                        TypeDBSubject(it[S_REQUESTED_TYPE], it[S_REQUESTED_ID_TYPE], it[S_REQUESTED_ID]),
                        TypeDBAccess(
                            TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                            TypeDBAction(it[A_TYPE], it[A_NAME])
                        )
                    )
                }
            }

            if (requests.isEmpty()) break
            val requestedSubject = requests.first().requestedSubject
            val accessedObject = requests.first().requestedAccess.accessedObject
            val validAction = requests.first().requestedAccess.validAction
            val directPermissionExists: Boolean

            session.transaction(READ).use { tx ->
                directPermissionExists = tx.query().match(
                    match(
                        cvar(S).isa(requestedSubject.type).has(requestedSubject.idType, requestedSubject.idValue),
                        cvar(O).isa(accessedObject.type).has(accessedObject.idType, accessedObject.idValue),
                        cvar(A).isa(validAction.type).has(validAction.idType, validAction.idValue),
                        cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                        rel(PERMITTED_SUBJECT, cvar(S)).rel(PERMITTED_ACCESS, cvar(AC)).isa(PERMISSION),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                    )
                ).toList().isNotEmpty()
            }

            if (directPermissionExists) {
                session.transaction(WRITE).use { tx ->
                    tx.query().delete(
                        match(
                            cvar(S).isa(requestedSubject.type).has(requestedSubject.idType, requestedSubject.idValue),
                            cvar(O).isa(accessedObject.type).has(accessedObject.idType, accessedObject.idValue),
                            cvar(A).isa(validAction.type).has(validAction.idType, validAction.idValue),
                            cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                            cvar(R).rel(REQUESTED_SUBJECT, cvar(S)).rel(REQUESTED_CHANGE, cvar(AC)).isa(CHANGE_REQUEST),
                            cvar(C).isa(COMPANY).has(NAME, company.name),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                        ).delete(
                            cvar(R).isa(CHANGE_REQUEST),
                        )
                    )

                    if (randomSource.nextInt(100) < context.model.requestApprovalPercentage) {
                        tx.query().delete(
                            match(
                                cvar(S).isa(requestedSubject.type)
                                    .has(requestedSubject.idType, requestedSubject.idValue),
                                cvar(O).isa(accessedObject.type).has(accessedObject.idType, accessedObject.idValue),
                                cvar(A).isa(validAction.type).has(validAction.idType, validAction.idValue),
                                cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                                cvar(P).rel(PERMITTED_SUBJECT, cvar(S)).rel(PERMITTED_ACCESS, cvar(AC)).isa(PERMISSION),
                                cvar(C).isa(COMPANY).has(NAME, company.name),
                                rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                                rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                                rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                            ).delete(
                                cvar(P).isa(PERMISSION)
                            )
                        )
                    }

                    tx.commit()
                }
            } else {
                session.transaction(WRITE).use { tx ->
                    tx.query().delete(
                        match(
                            cvar(S).isa(requestedSubject.type).has(requestedSubject.idType, requestedSubject.idValue),
                            cvar(O).isa(accessedObject.type).has(accessedObject.idType, accessedObject.idValue),
                            cvar(A).isa(validAction.type).has(validAction.idType, validAction.idValue),
                            cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                            cvar(R).rel(REQUESTED_SUBJECT, cvar(S)).rel(REQUESTED_CHANGE, cvar(AC)).isa(CHANGE_REQUEST),
                            cvar(C).isa(COMPANY).has(NAME, company.name),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                        ).delete(
                            cvar(R).isa(CHANGE_REQUEST),
                        )
                    )

                    if (randomSource.nextInt(100) < context.model.requestApprovalPercentage) {
                        tx.query().insert(
                            match(
                                cvar(S).isa(requestedSubject.type).has(requestedSubject.idType, requestedSubject.idValue),
                                cvar(O).isa(accessedObject.type).has(accessedObject.idType, accessedObject.idValue),
                                cvar(A).isa(validAction.type).has(validAction.idType, validAction.idValue),
                                cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                                cvar(C).isa(COMPANY).has(NAME, company.name),
                                rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                                rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                                rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                            ).insert(
                                rel(PERMITTED_SUBJECT, cvar(S)).rel(PERMITTED_ACCESS, cvar(AC)).isa(PERMISSION)
                                    .has(REVIEW_DATE, iterationDate(context.iterationNumber + context.model.permissionReviewAge)),
                            )
                        )
                    }

                    tx.commit()
                }
            }
        }

        return listOf()
    }

    override fun collectGarbage(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        session.transaction(WRITE).use { tx ->
            tx.query().delete(
                match(
                    cvar(AT).isa(ATTRIBUTE),
                    not(cvar().has(ATTRIBUTE, cvar(AT))),
                    not(cvar(AT).isa(PARENT_COMPANY_NAME)),
                ).delete(
                    cvar(AT).isa(ATTRIBUTE)
                )
            )

            tx.commit()
        }

        return listOf()
    }

    private fun deleteSubject(session: TypeDBSession, company: Company, randomSource: RandomSource, subjectType: TypeDBSubjectType) {
        val subject = getRandomEntity(session, company, randomSource, subjectType.label)?.asSubject() ?: return
        val newOwner = getRandomEntity(session, company, randomSource, subjectType.label)?.asSubject() ?: return
        val ownedGroups: List<TypeDBSubject>

        session.transaction(READ, options).use { tx ->
            ownedGroups = tx.query().match(
                match(
                    cvar(S).isaX(cvar(S_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(S_ID)),
                    cvar(S_ID).isaX(cvar(S_ID_TYPE)),
                    cvar(S_OWNER).isa(subject.type).has(PARENT_COMPANY_NAME, company.name).has(subject.idType, subject.idValue),
                    cvar(S_TYPE).sub(USER_GROUP),
                    cvar(S_ID_TYPE).sub(ID),
                    rel(OWNED_GROUP, cvar(S)).rel(GROUP_OWNER, cvar(S_OWNER)).isa(GROUP_OWNERSHIP),
                )
            ).toList().map { TypeDBSubject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]) }
        }

        val ownedObjects: List<TypeDBObject>

        session.transaction(READ, options).use { tx ->
            ownedObjects = tx.query().match(
                match(
                    cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(S).isa(subject.type).has(PARENT_COMPANY_NAME, company.name).has(subject.idType, subject.idValue),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    rel(OWNED_OBJECT, cvar(O)).rel(OBJECT_OWNER, cvar(S)).isa(OBJECT_OWNERSHIP),
                )
            ).toList().map { TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]) }
        }

        session.transaction(WRITE).use { tx ->
            ownedGroups.parallelStream().forEach { group ->
                tx.query().delete(
                    match(
                        cvar(S).isa(group.type).has(group.idType, group.idValue),
                        cvar(S_OWNER).isa(subject.type).has(subject.idType, subject.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(OW).rel(OWNED_GROUP, cvar(S)).rel(GROUP_OWNER, cvar(S_OWNER)).isa(GROUP_OWNERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S_OWNER)).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(OW).isa(GROUP_OWNERSHIP),
                    )
                )

                tx.query().insert(
                    match(
                        cvar(S).isa(group.type).has(group.idType, group.idValue),
                        cvar(S_OWNER).isa(newOwner.type).has(newOwner.idType, newOwner.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S_OWNER)).isa(COMPANY_MEMBERSHIP),
                    ).insert(
                        rel(OWNED_GROUP, cvar(S)).rel(GROUP_OWNER, cvar(S_OWNER)).isa(GROUP_OWNERSHIP),
                    )
                )
            }

            ownedObjects.parallelStream().forEach { obj ->
                tx.query().delete(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(S).isa(subject.type).has(subject.idType, subject.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        cvar(OW).rel(OWNED_OBJECT, cvar(O)).rel(OBJECT_OWNER, cvar(S)).isa(OBJECT_OWNERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(OW).isa(OBJECT_OWNERSHIP),
                    )
                )

                tx.query().insert(
                    match(
                        cvar(O).isa(obj.type).has(obj.idType, obj.idValue),
                        cvar(S).isa(newOwner.type).has(newOwner.idType, newOwner.idValue),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                    ).insert(
                        rel(OWNED_OBJECT, cvar(O)).rel(OBJECT_OWNER, cvar(S)).isa(OBJECT_OWNERSHIP),
                    )
                )
            }

            tx.query().delete(
                match(
                    cvar(S).isa(subject.type).has(subject.idType, subject.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(ME).rel(cvar(S)).isa(GROUP_MEMBERSHIP),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                ).delete(
                    cvar(ME).isa(GROUP_MEMBERSHIP),
                )
            )

            tx.query().delete(
                match(
                    cvar(S).isa(subject.type).has(subject.idType, subject.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(P).rel(PERMITTED_SUBJECT, cvar(S)).isa(PERMISSION),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                ).delete(
                    cvar(P).isa(PERMISSION),
                )
            )

            tx.query().delete(
                match(
                    cvar(S).isa(subject.type).has(subject.idType, subject.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(R).rel(cvar(S)).isa(CHANGE_REQUEST),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                ).delete(
                    cvar(R).isa(CHANGE_REQUEST),
                )
            )

            tx.query().delete(
                match(
                    cvar(S).isa(subject.type).has(subject.idType, subject.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(ME).rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                ).delete(
                    cvar(S).isa(subject.type),
                    cvar(ME).isa(COMPANY_MEMBERSHIP),
                )
            )

            tx.commit()
        }
    }

    companion object {
        private const val A = "a"
        private const val AC = "ac"
        private const val AT = "at"
        private const val A_NAME = "a-name"
        private const val A_TYPE = "a-type"
        private const val C = "c"
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
