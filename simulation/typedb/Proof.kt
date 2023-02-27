package com.vaticle.typedb.iam.simulation.typedb

import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.logic.Rule

data class Proof(val concepts: Set<Concept>, val rules: Set<Rule>) {
    fun isEqualTo(proof: Proof): Boolean {
        return this.concepts == proof.concepts && this.rules == proof.rules
    }

    fun isSubsetOf(proof: Proof): Boolean {
        return proof.concepts.containsAll(this.concepts) && proof.rules.containsAll(this.rules)
    }

    fun isSupersetOf(proof: Proof): Boolean {
        return proof.isSubsetOf(this)
    }

    fun isProperSubsetOf(proof: Proof): Boolean {
        return this.isSubsetOf(proof) &&! this.isEqualTo(proof)
    }

    fun isProperSupersetOf(proof: Proof): Boolean {
        return this.isSupersetOf(proof) &&! this.isEqualTo(proof)
    }

    fun unionWith(proof: Proof): Proof {
        return Proof(this.concepts + proof.concepts, this.rules + proof.rules)
    }

    companion object {
        fun unionOf(proofs: Collection<Proof>): Proof {
            var proof = Proof(setOf(), setOf())
            proofs.forEach { proof = proof.unionWith(it) }
            return proof
        }
    }
}