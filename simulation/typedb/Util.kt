package com.vaticle.typedb.iam.simulation.typedb

import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.iam.simulation.common.`object`.Company
import com.vaticle.typedb.iam.simulation.typedb.concept.TypeDBEntity
import com.vaticle.typedb.iam.simulation.typedb.Labels.ID
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY_NAME
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typeql.lang.TypeQL.*
import java.lang.IllegalArgumentException
import java.time.LocalDateTime

import kotlin.streams.toList

object Util {
    private val options: TypeDBOptions = TypeDBOptions.core().infer(true)

    fun typeLabel(typeConcept: Concept): String {
        assert(typeConcept.isType)
        return typeConcept.asType().label.name()
    }

    fun roleLabel(typeConcept: Concept): String {
        assert(typeConcept.isRoleType)
        return typeConcept.asRoleType().label.name()
    }

    fun thingLabel(thingConcept: Concept): String {
        assert(thingConcept.isThing)
        return typeLabel(thingConcept.asThing().type)
    }

    fun booleanValue(attributeConcept: Concept): Boolean {
        assert(attributeConcept.isAttribute)
        assert(attributeConcept.asAttribute().isBoolean)
        return attributeConcept.asAttribute().asBoolean().value
    }

    fun longValue(attributeConcept: Concept): Long {
        assert(attributeConcept.isAttribute)
        assert(attributeConcept.asAttribute().isLong)
        return attributeConcept.asAttribute().asLong().value
    }

    fun doubleValue(attributeConcept: Concept): Double {
        assert(attributeConcept.isAttribute)
        assert(attributeConcept.asAttribute().isDouble)
        return attributeConcept.asAttribute().asDouble().value
    }

    fun stringValue(attributeConcept: Concept): String {
        assert(attributeConcept.isAttribute)
        assert(attributeConcept.asAttribute().isString)
        return attributeConcept.asAttribute().asString().value
    }

    fun datetimeValue(attributeConcept: Concept): LocalDateTime {
        assert(attributeConcept.isAttribute)
        assert(attributeConcept.asAttribute().isDateTime)
        return attributeConcept.asAttribute().asDateTime().value
    }

    fun forceStringValue(attributeConcept: Concept): String {
        assert(attributeConcept.isAttribute)
        if(attributeConcept.asAttribute().isBoolean) return booleanValue(attributeConcept).toString()
        if(attributeConcept.asAttribute().isLong) return longValue(attributeConcept).toString()
        if(attributeConcept.asAttribute().isDouble) return doubleValue(attributeConcept).toString()
        if(attributeConcept.asAttribute().isString) return stringValue(attributeConcept)
        if(attributeConcept.asAttribute().isDateTime) return datetimeValue(attributeConcept).toString()
        throw IllegalArgumentException()
    }

    fun getRandomEntity(session: TypeDBSession, company: Company, randomSource: RandomSource, entityType: String): TypeDBEntity {
        val candidateEntities: List<TypeDBEntity>

        session.transaction(READ, options).use { transaction ->
            candidateEntities = transaction.query().match(
                match(
                    `var`(E).isa(entityType)
                        .has(PARENT_COMPANY_NAME, company.name)
                        .has(ID, E_ID),
                    `var`(E).isaX(E_TYPE),
                    `var`(E_ID).isaX(E_ID_TYPE)
                )
            ).toList().map { TypeDBEntity(it[E_TYPE], it[E_ID_TYPE], it[E_ID]) }
        }

        return randomSource.choose(candidateEntities)
    }

    fun getProofs(transaction: TypeDBTransaction, conceptMap: ConceptMap): Set<Proof> {
        val explainables = conceptMap.explainables().relations() + conceptMap.explainables().attributes() + conceptMap.explainables().ownerships()
            .filter { transaction.query().explain(it.value).toList().isNotEmpty() }
        val baseFacts = conceptMap.concepts().filterNot { it.asThing().isInferred }.toSet()

        if (explainables.isEmpty()) return setOf(Proof(baseFacts, setOf()))

        val proofsOfExplainables = mutableSetOf<MutableSet<Proof>>()

        explainables.forEach { explainable ->
            val explanations = transaction.query().explain(explainable.value)
            val proofsOfExplainable = mutableSetOf<Proof>()

            explanations.forEach { explanation ->
                val rule = explanation.rule()
                val condition = explanation.condition()
                val proofsOfCondition = getProofs(transaction, condition)

                proofsOfCondition.forEach { proofOfCondition ->
                    val proofOfExplainable = proofOfCondition.unionWith(Proof(setOf(), setOf(rule)))
                    proofsOfExplainable += proofOfExplainable
                }
            }

            proofsOfExplainables += proofsOfExplainable
        }

        val proofsOfConceptMap = mutableSetOf<Proof>()
        val proofSets = cartesianProduct(proofsOfExplainables.toList().map { it.toList() }).map { it.toSet() }.toSet()

        proofSets.forEach { proofSet ->
            val proofOfConceptMap = Proof.unionOf(proofSet).unionWith(Proof(baseFacts, setOf()))
            proofsOfConceptMap += proofOfConceptMap
        }

        return proofsOfConceptMap.filterNot { proof -> proofsOfConceptMap.map { it.isProperSubsetOf(proof) }.any() }.toSet()
    }

    fun decodeProof(proof: Proof, transaction: TypeDBTransaction): List<String> {
        val typeCounts = mutableMapOf<String, Int>()
        val bindings = mutableMapOf<Concept, String>()

        proof.concepts.forEach { concept ->
            val type = thingLabel(concept)

            when (type in typeCounts) {
                true -> typeCounts[type] = typeCounts[type]!! + 1
                false -> typeCounts[type] = 1
            }

            val binding = when (concept.isAttribute) {
                true -> when (concept.asAttribute().isString) {
                    true -> "\"" + forceStringValue(concept) + "\""
                    false -> forceStringValue(concept)
                }
                false -> "\$" + type + "-" + typeCounts[type].toString()
            }

            bindings[concept] = binding
        }

        val decodedProof = mutableListOf<String>()

        proof.concepts.forEach { concept ->
            if (concept.isEntity) {
                val attributes = concept.asEntity().asRemote(transaction).getHas().toList()
                val entityBinding = bindings[concept]
                val entityType = thingLabel(concept)

                val entityAttributes = attributes.joinToString(separator = "") { attribute ->
                    when (attribute in bindings) {
                        true -> ", has " + thingLabel(attribute) + " " + bindings[attribute]
                        false -> ""
                    }
                }

                decodedProof += "$entityBinding isa $entityType$entityAttributes;"
            }

            if (concept.isRelation) {
                val roles = concept.asRelation().asRemote(transaction).playersByRoleType
                val attributes = concept.asRelation().asRemote(transaction).getHas().toList()
                val relationBinding = bindings[concept]
                val relationType = thingLabel(concept)

                val relationRoleplayers = roles.keys.map { role ->
                   roles[role]!!.map { roleplayer ->
                       when (roleplayer in bindings) {
                           true -> roleLabel(role).split(":")[1] + ": " + bindings[roleplayer]
                           false -> ""
                       }
                   }
                }.flatten().joinToString(separator = ", ")

                val relationAttributes = attributes.joinToString(separator = "") { attribute ->
                    when (attribute in bindings) {
                        true -> ", has " + thingLabel(attribute) + " " + bindings[attribute]
                        false -> ""
                    }
                }

                decodedProof += "$relationBinding ($relationRoleplayers) isa $relationType;"
            }
        }

        return decodedProof
    }


    fun <T> cartesianProduct(lists: List<List<T>>): List<List<T>> {
        // Credit: https://gist.github.com/erikhuizinga/d2ca2b501864df219fd7f25e4dd000a4

        return lists.fold(listOf(listOf<T>())) { accumulator, elements ->
            accumulator.flatMap { list ->
                elements.map { element ->
                    list + element
                }
            }
        }
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