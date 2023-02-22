package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.iam.simulation.common.Util.stringValue
import com.vaticle.typedb.iam.simulation.common.Util.typeLabel
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.common.concept.Entity
import com.vaticle.typedb.iam.simulation.common.concept.Subject
import com.vaticle.typedb.iam.simulation.typedb.Labels.ID
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typeql.lang.TypeQL.match
import com.vaticle.typeql.lang.TypeQL.`var`
import kotlin.streams.toList

object Queries {
    private val options: TypeDBOptions = TypeDBOptions().infer(true)

    fun getRandomEntity(session: TypeDBSession, company: Company, randomSource: RandomSource, entityType: String): Entity {
        val candidateEntities: List<Subject>

        session.transaction(READ, options).use { transaction ->
            candidateEntities = transaction.query().match(
                match(
                    `var`(E).isa(entityType)
                        .has(PARENT_COMPANY, company.name)
                        .has(ID, E_ID),
                    `var`(E).isaX(E_TYPE),
                    `var`(E_ID).isaX(E_ID_TYPE)
                )
            ).toList().map { Subject(
                typeLabel(it[E_TYPE]),
                typeLabel(it[E_ID_TYPE]),
                stringValue(it[E_ID])
            ) }
        }

        return randomSource.choose(candidateEntities)
    }

    private const val A = "a"
    private const val AC = "ac"
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
    private const val R = "r"
    private const val S = "s"
    private const val S_ID = "s-id"
    private const val S_ID_TYPE = "s-id-type"
    private const val S_OWNER = "s-owner"
    private const val S_REQUESTED = "s-requested"
    private const val S_REQUESTING = "s-requesting"
    private const val S_TYPE = "s-type"
}