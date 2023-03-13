package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.agent.SupervisorAgent
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
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY_NAME
import com.vaticle.typedb.iam.simulation.typedb.Util.cvar
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBSubject
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import kotlin.streams.toList

class TypeDBSupervisorAgent(client: TypeDBClient, context:Context): SupervisorAgent<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions.core().infer(true)

    override fun assignGroupMembership(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val group = getRandomEntity(session, company, randomSource, USER_GROUP)?.asSubject() ?: return listOf<Report>()
        val member = getRandomEntity(session, company, randomSource, SUBJECT)?.asSubject() ?: return listOf<Report>()

        session.transaction(READ, options).use { tx ->
            if (
                tx.query().match(
                    match(
                        cvar(S).isa(group.type).has(group.idType, group.idValue).has(PARENT_COMPANY_NAME, company.name),
                        cvar(S_MEMBER).isa(member.type).has(member.idType, member.idValue).has(PARENT_COMPANY_NAME, company.name),
                        rel(PARENT_GROUP, S).rel(GROUP_MEMBER, S_MEMBER).isa(GROUP_MEMBERSHIP)
                    )
                ).toList().isNotEmpty()
            ) return listOf<Report>()
        }

        session.transaction(WRITE).use { tx ->
            tx.query().insert(
                match(
                    cvar(S).isa(group.type).has(group.idType, group.idValue),
                    cvar(S_MEMBER).isa(member.type).has(member.idType, member.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S_MEMBER).isa(COMPANY_MEMBERSHIP),
                ).insert(
                    rel(PARENT_GROUP, S).rel(GROUP_MEMBER, S_MEMBER).isa(GROUP_MEMBERSHIP),
                )
            )

            tx.commit()
        }

        return listOf<Report>()
    }

    override fun revokeGroupMembership(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val group = getRandomEntity(session, company, randomSource, USER_GROUP)?.asSubject() ?: return listOf<Report>()
        val candidateMembers: List<TypeDBSubject>

        session.transaction(READ, options).use { tx ->
            candidateMembers = tx.query().match(
                match(
                    cvar(S).isa(group.type).has(PARENT_COMPANY_NAME, company.name).has(group.idType, group.idValue),
                    cvar(S_MEMBER).isaX(cvar(S_MEMBER_TYPE)).has(PARENT_COMPANY_NAME, company.name).has(cvar(S_MEMBER_ID)),
                    cvar(S_MEMBER_ID).isaX(cvar(S_MEMBER_ID_TYPE)),
                    cvar(S_MEMBER_TYPE).sub(SUBJECT),
                    cvar(S_MEMBER_ID_TYPE).sub(ID),
                    rel(PARENT_GROUP, S).rel(GROUP_MEMBER, S_MEMBER).isa(GROUP_MEMBERSHIP),
                )
            ).toList().map { TypeDBSubject(it[S_MEMBER_TYPE], it[S_MEMBER_ID_TYPE], it[S_MEMBER_ID]) }
        }

        if (candidateMembers.isEmpty()) return listOf<Report>()
        val member = randomSource.choose(candidateMembers)

        session.transaction(WRITE).use { tx ->
            tx.query().delete(
                match(
                    cvar(S).isa(group.type).has(group.idType, group.idValue),
                    cvar(S_MEMBER).isa(member.type).has(member.idType, member.idValue),
                    cvar(C).isa(COMPANY).has(NAME, company.name),
                    cvar(ME).rel(PARENT_GROUP, S).rel(GROUP_MEMBER, S_MEMBER).isa(GROUP_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S_MEMBER).isa(COMPANY_MEMBERSHIP),
                ).delete(
                    cvar(ME).isa(GROUP_MEMBERSHIP),
                )
            )

            tx.commit()
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
