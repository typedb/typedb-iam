package com.vaticle.typedb.iam.simulation.common.concept

import com.vaticle.typedb.simulation.common.Partition
import com.vaticle.typedb.simulation.common.Util.buildTracker
import java.util.Objects

data class Company(override val name: String): Partition {
    override val code get() = name
    override val tracker get() = buildTracker(name)
    override val group get() = name
    val domainName = name.lowercase().replace(" ", "-").replace("&", "and").replace("'", "")
}