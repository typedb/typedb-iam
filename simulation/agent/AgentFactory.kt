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
import com.vaticle.typedb.simulation.Agent
import com.vaticle.typedb.simulation.common.DBClient

abstract class AgentFactory<CLIENT: DBClient<*>>(client: CLIENT, context: Context) : Agent.Factory() {

    override val map: Map<Class<out Agent<*, *, *>>, () -> Agent<*, *, *>> = mapOf(
        AssignChangeRequestAgent::class.java to { assignChangeRequestAgent(client, context) }, //user
        AssignCollectionMembershipAgent::class.java to { assignCollectionMembershipAgent(client, context) }, //only done on object creation
        AssignGroupMembershipAgent::class.java to { assignGroupMembershipAgent(client, context) }, //manager
        AssignPermissionAgent::class.java to { assignPermissionAgent(client, context) }, //never done
        AssignSegregationPolicyAgent::class.java to { assignSegregationPolicyAgent(client, context) }, //policy manager
        ChangeGroupOwnershipAgent::class.java to { changeGroupOwnershipAgent(client, context) }, //owner
        ChangeObjectOwnershipAgent::class.java to { changeObjectOwnershipAgent(client, context) }, //owner
        CheckCollectionMembershipAgent::class.java to { checkCollectionMembershipAgent(client, context) }, //never done
        CheckGroupMembershipAgent::class.java to { checkGroupMembershipAgent(client, context) }, //never done
        CheckPermissionAgent::class.java to { checkPermissionAgent(client, context) }, //user
        CreateResourceAgent::class.java to { createResourceAgent(client, context) }, //user
        CreateResourceCollectionAgent::class.java to { createResourceCollectionAgent(client, context) }, //user
        CreateUserAgent::class.java to { createUserAgent(client, context) }, //sysadmin
        CreateUserGroupAgent::class.java to { createUserGroupAgent(client, context) }, //sysadmin
        DeleteObjectAgent::class.java to { deleteObjectAgent(client, context) }, //user
        DeleteSubjectAgent::class.java to { deleteSubjectAgent(client, context) }, //sysadmin
        ListObjectCollectionMembershipAgent::class.java to { listObjectCollectionMembershipAgent(client, context) }, //never done
        ListPermissionAgent::class.java to { listPermissionAgent(client, context) }, //sysadmin
        ListPermissionPendingReviewAgent::class.java to { listPermissionPendingReviewAgent(client, context) }, //policy manager
        ListSegregationViolationAgent::class.java to { listSegregationViolationAgent(client, context) }, //policy manager
        ListSubjectGroupMembershipAgent::class.java to { listSubjectGroupMembershipAgent(client, context) }, //sysadmin
        ReviewChangeRequestAgent::class.java to { reviewChangeRequestAgent(client, context) }, //sysadmin
        ReviewPermissionAgent::class.java to { reviewPermissionAgent(client, context) }, //policy manager
        ReviewSegregationViolationAgent::class.java to { reviewSegregationViolationAgent(client, context) }, //policy manager
        RevokeCollectionMembershipAgent::class.java to { revokeCollectionMembershipAgent(client, context) }, //never done
        RevokeGroupMembershipAgent::class.java to { revokeGroupMembershipAgent(client, context) }, //manager
        RevokePermissionAgent::class.java to { revokePermissionAgent(client, context) }, //sysadmin
        RevokeSegregationPolicyAgent::class.java to { revokeSegregationPolicyAgent(client, context) } //policy manager
    )

    protected abstract fun assignChangeRequestAgent(client: CLIENT, context: Context): AssignChangeRequestAgent<*>
    protected abstract fun assignCollectionMembershipAgent(client: CLIENT, context: Context): AssignCollectionMembershipAgent<*>
    protected abstract fun assignGroupMembershipAgent(client: CLIENT, context: Context): AssignGroupMembershipAgent<*>
    protected abstract fun assignPermissionAgent(client: CLIENT, context: Context): AssignPermissionAgent<*>
    protected abstract fun assignSegregationPolicyAgent(client: CLIENT, context: Context): AssignSegregationPolicyAgent<*>
    protected abstract fun changeGroupOwnershipAgent(client: CLIENT, context: Context): ChangeGroupOwnershipAgent<*>
    protected abstract fun changeObjectOwnershipAgent(client: CLIENT, context: Context): ChangeObjectOwnershipAgent<*>
    protected abstract fun checkCollectionMembershipAgent(client: CLIENT, context: Context): CheckCollectionMembershipAgent<*>
    protected abstract fun checkGroupMembershipAgent(client: CLIENT, context: Context): CheckGroupMembershipAgent<*>
    protected abstract fun checkPermissionAgent(client: CLIENT, context: Context): CheckPermissionAgent<*>
    protected abstract fun createResourceAgent(client: CLIENT, context: Context): CreateResourceAgent<*>
    protected abstract fun createResourceCollectionAgent(client: CLIENT, context: Context): CreateResourceCollectionAgent<*>
    protected abstract fun createUserAgent(client: CLIENT, context: Context): CreateUserAgent<*>
    protected abstract fun createUserGroupAgent(client: CLIENT, context: Context): CreateUserGroupAgent<*>
    protected abstract fun deleteObjectAgent(client: CLIENT, context: Context): DeleteObjectAgent<*>
    protected abstract fun deleteSubjectAgent(client: CLIENT, context: Context): DeleteSubjectAgent<*>
    protected abstract fun listObjectCollectionMembershipAgent(client: CLIENT, context: Context): ListObjectCollectionMembershipAgent<*>
    protected abstract fun listPermissionAgent(client: CLIENT, context: Context): ListPermissionAgent<*>
    protected abstract fun listPermissionPendingReviewAgent(client: CLIENT, context: Context): ListPermissionPendingReviewAgent<*>
    protected abstract fun listSegregationViolationAgent(client: CLIENT, context: Context): ListSegregationViolationAgent<*>
    protected abstract fun listSubjectGroupMembershipAgent(client: CLIENT, context: Context): ListSubjectGroupMembershipAgent<*>
    protected abstract fun reviewChangeRequestAgent(client: CLIENT, context: Context): ReviewChangeRequestAgent<*>
    protected abstract fun reviewPermissionAgent(client: CLIENT, context: Context): ReviewPermissionAgent<*>
    protected abstract fun reviewSegregationViolationAgent(client: CLIENT, context: Context): ReviewSegregationViolationAgent<*>
    protected abstract fun revokeCollectionMembershipAgent(client: CLIENT, context: Context): RevokeCollectionMembershipAgent<*>
    protected abstract fun revokeGroupMembershipAgent(client: CLIENT, context: Context): RevokeGroupMembershipAgent<*>
    protected abstract fun revokePermissionAgent(client: CLIENT, context: Context): RevokePermissionAgent<*>
    protected abstract fun revokeSegregationPolicyAgent(client: CLIENT, context: Context): RevokeSegregationPolicyAgent<*>
}
