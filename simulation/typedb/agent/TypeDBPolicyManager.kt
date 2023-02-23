package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.agent.PolicyManager
import com.vaticle.typedb.iam.simulation.agent.SysAdmin
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.Util.stringValue
import com.vaticle.typedb.iam.simulation.common.Util.typeLabel
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
import com.vaticle.typedb.iam.simulation.typedb.Labels.OPERATION
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_GROUP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT
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
import com.vaticle.typedb.iam.simulation.typedb.Labels.SEGREGATED_ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.SEGREGATION_POLICY
import com.vaticle.typedb.iam.simulation.typedb.Labels.SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_GROUP
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALID_ACTION
import com.vaticle.typedb.iam.simulation.typedb.agent.Queries.getRandomEntity
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import java.lang.IllegalArgumentException
import kotlin.streams.toList

class TypeDBPolicyManager(client: TypeDBClient, context:Context): PolicyManager<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions().infer(true)
    override fun listPermissionsPendingReview(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val permissions: List<Permission>

        session.transaction(READ, options).use { transaction ->
            permissions = transaction.query().match(
                match(
                    `var`(S).isa(SUBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, S_ID),
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, O_ID),
                    `var`(A).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(ACTION_NAME, A_NAME),
                    `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    `var`(P).rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION)
                        .has(REVIEW_DATE, P_REVIEW_DATE),
                    `var`(P_REVIEW_DATE).lte(context.model.permissionReviewAge.toLong()),
                    `var`(S).isaX(S_TYPE),
                    `var`(S_ID).isaX(S_ID_TYPE),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE),
                    `var`(A).isaX(A_TYPE)
                )
            ).toList().map {
                Permission(
                    Subject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    Access(
                        Object(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                        Action(it[A_TYPE], it[A_NAME])
                    )
                )
            }
        }

        return listOf<Report>()
    }

    override fun reviewPermissions(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val permissions: List<Permission>

        session.transaction(READ, options).use { transaction ->
            permissions = transaction.query().match(
                match(
                    `var`(S).isa(SUBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, S_ID),
                    `var`(O).isa(OBJECT)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, O_ID),
                    `var`(A).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(ACTION_NAME, A_NAME),
                    `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                    `var`(P).rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION)
                        .has(REVIEW_DATE, P_REVIEW_DATE),
                    `var`(P_REVIEW_DATE).lte(context.model.permissionReviewAge.toLong()),
                    `var`(S).isaX(S_TYPE),
                    `var`(S_ID).isaX(S_ID_TYPE),
                    `var`(O).isaX(O_TYPE),
                    `var`(O_ID).isaX(O_ID_TYPE),
                    `var`(A).isaX(A_TYPE)
                )
            ).toList().map {
                Permission(
                    Subject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    Access(
                        Object(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                        Action(it[A_TYPE], it[A_NAME])
                    )
                )
            }
        }

        permissions.forEach { permission ->
            val permittedSubject = permission.permittedSubject
            val accessedObject = permission.permittedAccess.accessedObject
            val validAction = permission.permittedAccess.validAction

            session.transaction(WRITE, options).use { transaction ->
                transaction.query().delete(
                    match(
                        `var`(S).isa(permittedSubject.type)
                            .has(permittedSubject.idType, permittedSubject.idValue),
                        `var`(O).isa(accessedObject.type)
                            .has(accessedObject.idType, accessedObject.idValue),
                        `var`(A).isa(validAction.type)
                            .has(validAction.idType, validAction.idValue),
                        `var`(AC).rel(ACCESSED_OBJECT, O).rel(VALID_ACTION, A).isa(ACCESS),
                        `var`(P).rel(PERMITTED_SUBJECT, S).rel(PERMITTED_ACCESS, AC).isa(PERMISSION),
                        `var`(C).isa(COMPANY)
                            .has(NAME, company.name),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A).isa(COMPANY_MEMBERSHIP)
                    ).delete(
                        `var`(P).isa(PERMISSION)
                    )
                )

                if (randomSource.nextInt(100) < context.model.permissionRenewalPercentage) {
                    transaction.query().insert(
                        match(
                            `var`(S).isa(permittedSubject.type)
                                .has(permittedSubject.idType, permittedSubject.idValue),
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

    override fun assignSegregationPolicy(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val action1 = getRandomEntity(session, company, randomSource, OPERATION).asAction()
        val action2 = getRandomEntity(session, company, randomSource, OPERATION).asAction()

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(A1).isa(action1.type)
                        .has(action1.idType, action1.idValue),
                    `var`(A2).isa(action2.type)
                        .has(action2.idType, action2.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A1).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A2).isa(COMPANY_MEMBERSHIP)
                ).insert(
                    rel(SEGREGATED_ACTION, A1).rel(SEGREGATED_ACTION, A2).isa(SEGREGATION_POLICY)
                )
            )
        }

        return listOf<Report>()
    }

    override fun listSegregationPolicies(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val segregationPolicies: List<SegregationPolicy>

        session.transaction(READ, options).use { transaction ->
            segregationPolicies = transaction.query().match(
                match(
                    `var`(A1).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A1_NAME),
                    `var`(A2).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A2_NAME),
                    `var`(A1).isaX(A1_TYPE),
                    `var`(A2).isaX(A2_TYPE)
                )
            ).toList().map {
                SegregationPolicy(
                    Action(it[A1_TYPE], it[A1_NAME]),
                    Action(it[A1_TYPE], it[A1_NAME])
                )
            }
        }

        return listOf<Report>()
    }

    override fun revokeSegregationPolicy(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val segregationPolicies: List<SegregationPolicy>

        session.transaction(READ, options).use { transaction ->
            segregationPolicies = transaction.query().match(
                match(
                    `var`(A1).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A1_NAME),
                    `var`(A2).isa(ACTION)
                        .has(PARENT_COMPANY, company.name)
                        .has(NAME, A2_NAME),
                    `var`(A1).isaX(A1_TYPE),
                    `var`(A2).isaX(A2_TYPE)
                )
            ).toList().map {
                SegregationPolicy(
                    Action(it[A1_TYPE], it[A1_NAME]),
                    Action(it[A1_TYPE], it[A1_NAME])
                )
            }
        }

        val segregationPolicy = randomSource.choose(segregationPolicies)
        val action1 = segregationPolicy.action1
        val action2 = segregationPolicy.action2

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().delete(
                match(
                    `var`(A1).isa(action1.type)
                        .has(action1.idType, action1.idValue),
                    `var`(A2).isa(action2.type)
                        .has(action2.idType, action2.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A1).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, A2).isa(COMPANY_MEMBERSHIP),
                    `var`(SP).rel(SEGREGATED_ACTION, A1).rel(SEGREGATED_ACTION, A2).isa(SEGREGATION_POLICY)
                ).delete(
                    `var`(SP).isa(SEGREGATION_POLICY)
                )
            )
        }

        return listOf<Report>()
    }

    override fun listSegregationViolations(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val segregationViolations: List<>
    }

    override fun reviewSegregationViolations(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        TODO("Not yet implemented")
    }

    companion object {
        private const val A = "a"
        private const val A1 = "a1"
        private const val A1_NAME = "a1-name"
        private const val A1_TYPE = "a1-type"
        private const val A2 = "a2"
        private const val A2_NAME = "a2-name"
        private const val A2_TYPE = "a2-type"
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
        private const val O_MEMBER = "o-member"
        private const val O_MEMBER_ID = "o-member-id"
        private const val O_MEMBER_ID_TYPE = "o-member-id-type"
        private const val O_MEMBER_TYPE = "o-member-type"
        private const val O_TYPE = "o-type"
        private const val P = "p"
        private const val P_REVIEW_DATE = "p-review-date"
        private const val R = "r"
        private const val S = "s"
        private const val SP = "sp"
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
