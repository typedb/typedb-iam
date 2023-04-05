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
package com.vaticle.typedb.iam.simulation.common

import com.vaticle.typedb.common.yaml.YAML
import com.vaticle.typedb.iam.simulation.common.Util.boolean
import com.vaticle.typedb.iam.simulation.common.Util.int
import com.vaticle.typedb.iam.simulation.common.Util.map

class ModelParams private constructor(val requestApprovalPercentage: Int, val permissionReviewAge: Int, val permissionRenewalPercentage: Int, val createDemoConcepts: Boolean) {
    companion object {
        private const val REQUEST_APPROVAL_PERCENTAGE = "requestApprovalPercentage"
        private const val PERMISSION_REVIEW_AGE = "permissionReviewAge"
        private const val PERMISSION_RENEWAL_PERCENTAGE = "permissionRenewalPercentage"
        private const val CREATE_DEMO_CONCEPTS = "createDemoConcepts"

        fun of(yaml: YAML.Map): ModelParams {
            val requestApprovalPercentage = int(map(yaml["model"])[REQUEST_APPROVAL_PERCENTAGE])
            val permissionReviewAge = int(map(yaml["model"])[PERMISSION_REVIEW_AGE])
            val permissionRenewalPercentage = int(map(yaml["model"])[PERMISSION_RENEWAL_PERCENTAGE])
            val createDemoConcepts = boolean(map(yaml["model"])[CREATE_DEMO_CONCEPTS])
            return ModelParams(requestApprovalPercentage, permissionReviewAge, permissionRenewalPercentage, createDemoConcepts)
        }
    }
}
