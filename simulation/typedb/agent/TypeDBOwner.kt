package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.agent.Owner
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.typedb.Util.getRandomEntity
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.GROUP_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_GROUP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.SUBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.USER_GROUP
import com.vaticle.typedb.iam.simulation.common.`object`.Company
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*

class TypeDBOwner(client: TypeDBClient, context:Context): Owner<TypeDBSession>(client, context) {
    private val options: TypeDBOptions = TypeDBOptions.core().infer(true)

    override fun changeGroupOwnership(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val group = getRandomEntity(session, company, randomSource, USER_GROUP).asSubject()
        val owner = getRandomEntity(session, company, randomSource, SUBJECT).asSubject()

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().delete(
                match(
                    `var`(S).isa(group.type)
                        .has(group.idType, group.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    rel(OWNED_GROUP, S).isa(GROUP_OWNERSHIP)
                ).delete(
                    rel(OWNED_GROUP, S).isa(GROUP_OWNERSHIP)
                )
            )

            transaction.query().insert(
                match(
                    `var`(S).isa(group.type)
                        .has(group.idType, group.idValue),
                    `var`(S_OWNER).isa(owner.type)
                        .has(owner.idType, owner.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, S_OWNER).isa(COMPANY_MEMBERSHIP)
                ).insert(
                    rel(OWNED_GROUP, S).rel(GROUP_OWNER, S_OWNER).isa(GROUP_OWNERSHIP)
                )
            )

            transaction.commit()
        }

        return listOf<Report>()
    }

    override fun changeObjectOwnership(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val `object` = getRandomEntity(session, company, randomSource, OBJECT).asObject()
        val owner = getRandomEntity(session, company, randomSource, SUBJECT).asSubject()

        session.transaction(WRITE, options).use { transaction ->
            transaction.query().delete(
                match(
                    `var`(O).isa(`object`.type)
                        .has(`object`.idType, `object`.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    rel(OWNED_OBJECT, O).isa(OBJECT_OWNERSHIP)
                ).delete(
                    rel(OWNED_OBJECT, O).isa(OBJECT_OWNERSHIP)
                )
            )

            transaction.query().insert(
                match(
                    `var`(O).isa(`object`.type)
                        .has(`object`.idType, `object`.idValue),
                    `var`(O_OWNER).isa(owner.type)
                        .has(owner.idType, owner.idValue),
                    `var`(C).isa(COMPANY)
                        .has(NAME, company.name),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O).isa(COMPANY_MEMBERSHIP),
                    rel(PARENT_COMPANY, C).rel(COMPANY_MEMBER, O_OWNER).isa(COMPANY_MEMBERSHIP)
                ).insert(
                    rel(OWNED_OBJECT, O).rel(OBJECT_OWNER, O_OWNER).isa(OBJECT_OWNERSHIP)
                )
            )

            transaction.commit()
        }

        return listOf<Report>()
    }

    companion object {
        private const val C = "c"
        private const val O = "o"
        private const val O_OWNER = "o-owner"
        private const val S = "s"
        private const val S_OWNER = "s-owner"
    }
}
