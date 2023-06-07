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
package com.vaticle.typedb.iam.simulation.agent

import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.benchmark.framework.Agent
import com.vaticle.typedb.benchmark.framework.common.DBClient
import com.vaticle.typedb.benchmark.framework.common.seed.RandomSource

abstract class SysAdminAgent<SESSION> protected constructor(client: DBClient<SESSION>, context: Context) :
    Agent<Company, SESSION, Context>(client, context) {
    override val agentClass = SysAdminAgent::class.java
    override val partitions = context.seedData.companies

    override val actionHandlers = mapOf(
        "addUser" to::addUser,
        "removeUser" to::removeUser,
        "createUserGroup" to::createUserGroup,
        "deleteUserGroup" to::deleteUserGroup,
        "listSubjectGroupMemberships" to::listSubjectGroupMemberships,
        "listSubjectPermissions" to::listSubjectPermissions,
        "listObjectPermissionHolders" to::listObjectPermissionHolders,
        "reviewChangeRequests" to::reviewChangeRequests,
        "collectGarbage" to::collectGarbage
    )

    protected abstract fun addUser(session: SESSION, company: Company, randomSource: RandomSource): List<Report>
    protected abstract fun removeUser(session: SESSION, company: Company, randomSource: RandomSource): List<Report>
    protected abstract fun createUserGroup(session: SESSION, company: Company, randomSource: RandomSource): List<Report>
    protected abstract fun deleteUserGroup(session: SESSION, company: Company, randomSource: RandomSource): List<Report>
    protected abstract fun listSubjectGroupMemberships(session: SESSION, company: Company, randomSource: RandomSource): List<Report>
    protected abstract fun listSubjectPermissions(session: SESSION, company: Company, randomSource: RandomSource): List<Report>
    protected abstract fun listObjectPermissionHolders(session: SESSION, company: Company, randomSource: RandomSource): List<Report>
    protected abstract fun reviewChangeRequests(session: SESSION, company: Company, randomSource: RandomSource): List<Report>
    protected abstract fun collectGarbage(session: SESSION, company: Company, randomSource: RandomSource): List<Report>
}
