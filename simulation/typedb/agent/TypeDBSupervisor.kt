package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.agent.Supervisor
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.typedb.Util.getRandomEntity
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.ID
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_GROUP
import com.vaticle.typedb.iam.simulation.typedb.Labels.SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_GROUP
import com.vaticle.typedb.iam.simulation.common.`object`.Company
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY_NAME
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBSubject
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import kotlin.streams.toList

class TypeDBSupervisor(client: TypeDBClient, context:Context): Supervisor<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions.core().infer(true)

    override fun assignGroupMembership(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val group = getRandomEntity(session, company, randomSource, USER_GROUP)?.asSubject() ?: return listOf<Report>()
        val member = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf<Report>()

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(S).isa(group.type)
                        .has(group.idType, group.idValue),
                    `var`(S_MEMBER).isa(member.type)
                        .has(member.idType, member.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S_MEMBER).isa(COMPANY_MEMBERSHIP)
                ).insert(
                    rel(PARENT_GROUP, S).rel(GROUP_MEMBER, S_MEMBER).isa(GROUP_MEMBERSHIP)
                )
            )

            transaction.commit()
        }

        return listOf<Report>()
    }

    override fun revokeGroupMembership(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val group = getRandomEntity(session, company, randomSource, USER_GROUP)?.asSubject() ?: return listOf<Report>()
        val candidateMembers: List<TypeDBSubject>

        session.transaction(READ, options).use { transaction ->
            candidateMembers = transaction.query().match(
                match(
                    `var`(S).isa(group.type)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(group.idType, group.idValue),
                    `var`(S_MEMBER).isaX(`var`(S_MEMBER_TYPE))
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(`var`(S_MEMBER_ID)),
                    `var`(S_MEMBER_ID).isaX(`var`(S_MEMBER_ID_TYPE)),
                    rel(PARENT_GROUP, S).rel(GROUP_MEMBER, S_MEMBER).isa(GROUP_MEMBERSHIP),
                    `var`(S_MEMBER_TYPE).sub(SUBJECT),
                    `var`(S_MEMBER_ID_TYPE).sub(ID)
                )
            ).toList().map { TypeDBSubject(it[S_MEMBER_TYPE], it[S_MEMBER_ID_TYPE], it[S_MEMBER_ID]) }
        }

        if (candidateMembers.isEmpty()) return listOf<Report>()
        val member = randomSource.choose(candidateMembers)

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().delete(
                match(
                    `var`(S).isa(group.type)
                        .has(group.idType, group.idValue),
                    `var`(S_MEMBER).isa(member.type)
                        .has(member.idType, member.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S_MEMBER).isa(COMPANY_MEMBERSHIP),
                    `var`(ME).rel(PARENT_GROUP, S).rel(GROUP_MEMBER, S_MEMBER).isa(GROUP_MEMBERSHIP)
                ).delete(
                    `var`(ME).isa(GROUP_MEMBERSHIP)
                )
            )

            transaction.commit()
        }

        return listOf<Report>()
    }

    companion object {
        private const val C = "c"
        private const val ME = "me"
        private const val S = "s"
        private const val S_MEMBER = "s-member"
        private const val S_MEMBER_ID = "s-member-id"
        private const val S_MEMBER_TYPE = "s-member-type"
        private const val S_MEMBER_ID_TYPE = "s-member-id-type"
    }
}
