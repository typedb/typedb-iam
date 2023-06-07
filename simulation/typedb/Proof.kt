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