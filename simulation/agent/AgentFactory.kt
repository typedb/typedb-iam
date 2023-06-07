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
import com.vaticle.typedb.benchmark.framework.Agent
import com.vaticle.typedb.benchmark.framework.common.DBClient

abstract class AgentFactory<CLIENT: DBClient<*>>(client: CLIENT, context: Context): Agent.Factory() {

    override val map: Map<Class<out Agent<*, *, *>>, () -> Agent<*, *, *>> = mapOf(
        UserAgent::class.java to { user(client, context) },
        OwnerAgent::class.java to { owner(client, context) },
        SupervisorAgent::class.java to { supervisor(client, context) },
        PolicyManagerAgent::class.java to { policyManager(client, context) },
        SysAdminAgent::class.java to { sysAdmin(client, context) }
    )

    protected abstract fun user(client: CLIENT, context: Context): UserAgent<*>
    protected abstract fun owner(client: CLIENT, context: Context): OwnerAgent<*>
    protected abstract fun supervisor(client: CLIENT, context: Context): SupervisorAgent<*>
    protected abstract fun policyManager(client: CLIENT, context: Context): PolicyManagerAgent<*>
    protected abstract fun sysAdmin(client: CLIENT, context: Context): SysAdminAgent<*>
}
