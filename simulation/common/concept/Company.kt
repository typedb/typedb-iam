package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.simulation.common.Partition
import com.vaticle.typedb.simulation.common.Util.buildTracker
import java.util.Objects

class Company(
    override val code: String,
    override val name: String
): Partition {
    private val hash = Objects.hash(code)
    override val tracker get(): String = buildTracker(name)
    override val group get(): String = name
    override fun toString(): String = "$name($code)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as Company
        return this.code == that.code
    }

    override fun hashCode(): Int {
        return hash
    }
}