package com.vaticle.typedb.iam.simulation.agent

import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.ModelParams
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.simulation.Agent
import com.vaticle.typedb.simulation.common.DBClient

abstract class AssignSegregationPolicyAgent<SESSION> protected constructor(client: DBClient<SESSION>, context: Context) :
    Agent<Company, SESSION, ModelParams>(client, context) {
    override val agentClass = AssignSegregationPolicyAgent::class.java
    override val partitions = context.seedData.companies
}