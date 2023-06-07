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
import com.vaticle.typedb.iam.simulation.agent.PolicyManagerAgent
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.Util.iterationDate
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACCESS
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACCESSED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.ACTION_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.ID
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.OPERATION
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMISSION
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMITTED_ACCESS
import com.vaticle.typedb.iam.simulation.typedb.Labels.PERMITTED_SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.REVIEW_DATE
import com.vaticle.typedb.iam.simulation.typedb.Labels.SEGREGATED_ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.SEGREGATION_POLICY
import com.vaticle.typedb.iam.simulation.typedb.Labels.SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALIDITY
import com.vaticle.typedb.iam.simulation.typedb.Labels.VALID_ACTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.VIOLATING_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.VIOLATED_POLICY
import com.vaticle.typedb.iam.simulation.typedb.Labels.VIOLATING_SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Proof
import com.vaticle.typedb.iam.simulation.typedb.Util.getProofs
import com.vaticle.typedb.iam.simulation.typedb.Util.getRandomEntity
import com.vaticle.typedb.iam.simulation.typedb.concept.*
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.POLICY_NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.SEGREGATION_VIOLATION
import com.vaticle.typedb.iam.simulation.typedb.Util.cvar
import com.vaticle.typedb.benchmark.framework.common.seed.RandomSource
import com.vaticle.typedb.benchmark.framework.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import java.lang.Exception
import kotlin.streams.toList

class TypeDBPolicyManagerAgent(client: TypeDBClient, context:Context): PolicyManagerAgent<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions.core().infer(true)
    override fun listPermissionsPendingReview(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val permissions: List<TypeDBPermission>

        session.transaction(READ, options).use { tx ->
            permissions = tx.query().match(
                match(
                    cvar(S).isaX(cvar(S_TYPE)).has(PARENT_COMPANY_NAME, company.name)
                        .has(cvar(S_ID)),
                    cvar(S_ID).isaX(cvar(S_ID_TYPE)),
                    cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(A).isaX(cvar(A_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A_NAME)),
                    cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                    cvar(P).rel(PERMITTED_SUBJECT, cvar(S)).rel(PERMITTED_ACCESS, cvar(AC)).isa(PERMISSION)
                        .has(VALIDITY, cvar(P_VALIDITY))
                        .has(REVIEW_DATE, cvar(P_DATE)),
                    cvar(P_DATE).lte(iterationDate(context.iterationNumber)),
                    cvar(S_TYPE).sub(SUBJECT),
                    cvar(S_ID_TYPE).sub(ID),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    cvar(A_TYPE).sub(ACTION),
                )
            ).toList().map {
                TypeDBPermission(
                    TypeDBSubject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
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

    override fun reviewPermissions(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val permissions: List<TypeDBPermission>

        session.transaction(READ, options).use { tx ->
            permissions = tx.query().match(
                match(
                    cvar(S).isaX(cvar(S_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(S_ID)),
                    cvar(S_ID).isaX(cvar(S_ID_TYPE)),
                    cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(A).isaX(cvar(A_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A_NAME)),
                    cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                    cvar(P).rel(PERMITTED_SUBJECT, cvar(S)).rel(PERMITTED_ACCESS, cvar(AC)).isa(PERMISSION)
                        .has(VALIDITY, cvar(P_VALIDITY))
                        .has(REVIEW_DATE, cvar(P_DATE)),
                    cvar(P_DATE).lte(iterationDate(context.iterationNumber)),
                    cvar(S_TYPE).sub(SUBJECT),
                    cvar(S_ID_TYPE).sub(ID),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    cvar(A_TYPE).sub(ACTION),
                )
            ).toList().map {
                TypeDBPermission(
                    TypeDBSubject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    TypeDBAccess(
                        TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                        TypeDBAction(it[A_TYPE], it[A_NAME])
                    ),
                    it[P_VALIDITY],
                    it[P_DATE]
                )
            }
        }

        permissions.forEach { permission ->
            val permittedSubject = permission.permittedSubject
            val accessedObject = permission.permittedAccess.accessedObject
            val validAction = permission.permittedAccess.validAction

            session.transaction(WRITE).use { tx ->
                tx.query().delete(
                    match(
                        cvar(S).isa(permittedSubject.type).has(permittedSubject.idType, permittedSubject.idValue),
                        cvar(O).isa(accessedObject.type).has(accessedObject.idType, accessedObject.idValue),
                        cvar(A).isa(validAction.type).has(validAction.idType, validAction.idValue),
                        cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                        cvar(P).rel(PERMITTED_SUBJECT, cvar(S)).rel(PERMITTED_ACCESS, cvar(AC)).isa(PERMISSION),
                        cvar(C).isa(COMPANY).has(NAME, company.name),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                        rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                    ).delete(
                        cvar(P).isa(PERMISSION),
                    )
                )

                if (randomSource.nextInt(100) < context.model.permissionRenewalPercentage) {
                    tx.query().insert(
                        match(
                            cvar(S).isa(permittedSubject.type).has(permittedSubject.idType, permittedSubject.idValue),
                            cvar(O).isa(accessedObject.type).has(accessedObject.idType, accessedObject.idValue),
                            cvar(A).isa(validAction.type).has(validAction.idType, validAction.idValue),
                            cvar(AC).rel(ACCESSED_OBJECT, cvar(O)).rel(VALID_ACTION, cvar(A)).isa(ACCESS),
                            cvar(C).isa(COMPANY).has(NAME, company.name),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(S)).isa(COMPANY_MEMBERSHIP),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(O)).isa(COMPANY_MEMBERSHIP),
                            rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A)).isa(COMPANY_MEMBERSHIP),
                        ).insert(
                            rel(PERMITTED_SUBJECT, cvar(S)).rel(PERMITTED_ACCESS, cvar(AC)).isa(PERMISSION)
                                .has(REVIEW_DATE, iterationDate(context.iterationNumber + context.model.permissionReviewAge))
                        )
                    )
                }

                tx.commit()
            }
        }

        return listOf()
    }

    override fun assignSegregationPolicy(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val action1 = getRandomEntity(session, company, randomSource, OPERATION)?.asAction() ?: return listOf()
        val action2 = getRandomEntity(session, company, randomSource, OPERATION)?.asAction() ?: return listOf()
        if (action1.idValue == action2.idValue) return listOf()
        val policyName = "'${action1.idValue}' or '${action2.idValue}' policy"

        session.transaction(READ, options).use { tx ->
            if (
                tx.query().match(
                    match(
                        cvar(A1).isa(action1.type).has(action1.idType, action1.idValue).has(PARENT_COMPANY_NAME, company.name),
                        cvar(A2).isa(action2.type).has(action2.idType, action2.idValue).has(PARENT_COMPANY_NAME, company.name),
                        rel(SEGREGATED_ACTION, cvar(A1)).rel(SEGREGATED_ACTION, cvar(A2)).isa(SEGREGATION_POLICY).has(POLICY_NAME, policyName)
                    )
                ).toList().isNotEmpty()
            ) return listOf()
        }

        session.transaction(WRITE).use { tx ->
            tx.query().insert(
                match(
                    cvar(A1).isa(action1.type).has(action1.idType, action1.idValue),
                    cvar(A2).isa(action2.type).has(action2.idType, action2.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A1)).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A2)).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    rel(SEGREGATED_ACTION, cvar(A1)).rel(SEGREGATED_ACTION, cvar(A2)).isa(SEGREGATION_POLICY).has(POLICY_NAME, policyName),
                )
            )

            tx.commit()
        }

        return listOf()
    }

    override fun listSegregationPolicies(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val segregationPolicies: List<TypeDBSegregationPolicy>

        session.transaction(READ, options).use { tx ->
            segregationPolicies = tx.query().match(
                match(
                    cvar(A1).isaX(cvar(A1_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A1_NAME)),
                    cvar(A2).isaX(cvar(A2_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A2_NAME)),
                    cvar(A1_NAME).lt(cvar(A2_NAME)),
                    cvar(A1_TYPE).sub(ACTION),
                    cvar(A2_TYPE).sub(ACTION),
                    rel(SEGREGATED_ACTION, cvar(A1)).rel(SEGREGATED_ACTION, cvar(A2)).isa(SEGREGATION_POLICY).has(POLICY_NAME, SP_NAME),
                )
            ).toList().map {
                TypeDBSegregationPolicy(
                    TypeDBAction(it[A1_TYPE], it[A1_NAME]),
                    TypeDBAction(it[A2_TYPE], it[A2_NAME]),
                    it[SP_NAME]
                )
            }
        }

        return listOf()
    }

    override fun revokeSegregationPolicy(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val segregationPolicies: List<TypeDBSegregationPolicy>

        session.transaction(READ, options).use { tx ->
            segregationPolicies = tx.query().match(
                match(
                    cvar(A1).isaX(cvar(A1_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A1_NAME)),
                    cvar(A2).isaX(cvar(A2_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A2_NAME)),
                    cvar(A1_NAME).lt(cvar(A2_NAME)),
                    cvar(A1_TYPE).sub(ACTION),
                    cvar(A2_TYPE).sub(ACTION),
                    rel(SEGREGATED_ACTION, cvar(A1)).rel(SEGREGATED_ACTION, cvar(A2)).isa(SEGREGATION_POLICY).has(POLICY_NAME, SP_NAME),
                )
            ).toList().map {
                TypeDBSegregationPolicy(
                    TypeDBAction(it[A1_TYPE], it[A1_NAME]),
                    TypeDBAction(it[A2_TYPE], it[A2_NAME]),
                    it[SP_NAME]
                )
            }
        }

        if (segregationPolicies.isEmpty()) return listOf()
        val segregationPolicy = randomSource.choose(segregationPolicies)
        val action1 = segregationPolicy.action1
        val action2 = segregationPolicy.action2

        session.transaction(WRITE).use { tx ->
            tx.query().delete(
                match(
                    cvar(A1).isa(action1.type).has(action1.idType, action1.idValue),
                    cvar(A2).isa(action2.type).has(action2.idType, action2.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(SP).rel(SEGREGATED_ACTION, cvar(A1)).rel(SEGREGATED_ACTION, cvar(A2)).isa(SEGREGATION_POLICY),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A1)).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, cvar(C)).rel(COMPANY_MEMBER, cvar(A2)).isa(COMPANY_MEMBERSHIP),
                ).delete(
                    cvar(SP).isa(SEGREGATION_POLICY),
                )
            )

            tx.commit()
        }

        return listOf()
    }

    override fun listSegregationViolations(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val segregationViolations: List<TypeDBSegregationViolation>

        session.transaction(READ, options).use { tx ->
            segregationViolations = tx.query().match(
                match(
                    cvar(S).isaX(cvar(S_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(S_ID)),
                    cvar(S_ID).isaX(cvar(S_ID_TYPE)),
                    cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(A1).isa(ACTION).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A1_NAME)),
                    cvar(A2).isa(ACTION).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A2_NAME)),
                    cvar(SP).rel(SEGREGATED_ACTION, cvar(A1)).rel(SEGREGATED_ACTION, cvar(A2)).isa(SEGREGATION_POLICY).has(POLICY_NAME, SP_NAME),
                    cvar(A1_NAME).lt(cvar(A2_NAME)),
                    cvar(S_TYPE).sub(SUBJECT),
                    cvar(S_ID_TYPE).sub(ID),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    rel(VIOLATING_SUBJECT, cvar(S)).rel(VIOLATING_OBJECT, cvar(O)).rel(cvar(VIOLATED_POLICY)).isa(SEGREGATION_VIOLATION),
                )
            ).toList().map {
                TypeDBSegregationViolation(
                    TypeDBSubject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                    TypeDBSegregationPolicy(
                        TypeDBAction(it[A1_TYPE], it[A1_NAME]),
                        TypeDBAction(it[A2_TYPE], it[A2_NAME]),
                        it[SP_NAME]
                    )
                )
            }
        }

        return listOf()
    }

    override fun reviewSegregationViolations(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        throw Exception("TypeDBPolicyManagerAgent.reviewSegregationViolations is not yet implemented.")

        val segregationViolations: Map<TypeDBSegregationViolation, Set<Proof>>

        session.transaction(READ, options).use { tx ->
            segregationViolations = tx.query().match(
                match(
                    cvar(S).isaX(cvar(S_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(S_ID)),
                    cvar(S_ID).isaX(cvar(S_ID_TYPE)),
                    cvar(O).isaX(cvar(O_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(O_ID)),
                    cvar(O_ID).isaX(cvar(O_ID_TYPE)),
                    cvar(A1).isa(ACTION).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A1_NAME)),
                    cvar(A2).isa(ACTION).has(PARENT_COMPANY_NAME, company.name).has(ACTION_NAME, cvar(A2_NAME)),
                    cvar(SP).rel(SEGREGATED_ACTION, cvar(A1)).rel(SEGREGATED_ACTION, cvar(A2)).isa(SEGREGATION_POLICY).has(POLICY_NAME, SP_NAME),
                    cvar(A1_NAME).lt(cvar(A2_NAME)),
                    cvar(S_TYPE).sub(SUBJECT),
                    cvar(S_ID_TYPE).sub(ID),
                    cvar(O_TYPE).sub(OBJECT),
                    cvar(O_ID_TYPE).sub(ID),
                    rel(VIOLATING_SUBJECT, cvar(S)).rel(VIOLATING_OBJECT, cvar(O)).rel(cvar(VIOLATED_POLICY)).isa(SEGREGATION_VIOLATION),
                )
            ).toList().map {
                TypeDBSegregationViolation(
                    TypeDBSubject(it[S_TYPE], it[S_ID_TYPE], it[S_ID]),
                    TypeDBObject(it[O_TYPE], it[O_ID_TYPE], it[O_ID]),
                    TypeDBSegregationPolicy(
                        TypeDBAction(it[A1_TYPE], it[A1_NAME]),
                        TypeDBAction(it[A2_TYPE], it[A2_NAME]),
                        it[SP_NAME]
                    )
                ) to getProofs(tx, it)
            }.toMap()
        }

        return listOf()
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
        private const val A_NAME = "a-name"
        private const val A_TYPE = "a-type"
        private const val C = "c"
        private const val O = "o"
        private const val O_ID = "o-id"
        private const val O_ID_TYPE = "o-id-type"
        private const val O_TYPE = "o-type"
        private const val P = "p"
        private const val P_DATE = "p-date"
        private const val P_VALIDITY = "p-validity"
        private const val S = "s"
        private const val SP = "sp"
        private const val SP_NAME = "sp-name"
        private const val S_ID = "s-id"
        private const val S_ID_TYPE = "s-id-type"
        private const val S_TYPE = "s-type"
    }
}
