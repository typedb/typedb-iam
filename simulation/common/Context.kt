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

import mu.KotlinLogging

class Context private constructor(seedData: SeedData, config: Config, isTracing: Boolean, isReporting: Boolean):
    com.vaticle.typedb.benchmark.framework.Context<SeedData, ModelParams>(seedData, config, isTracing, isReporting) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}

        fun create(config: Config, isTracing: Boolean, isReporting: Boolean): Context {
            val seedData = SeedData(config)
            LOGGER.info("Total number of companies in seed: {}", seedData.companies.size)
            return Context(seedData, config, isTracing, isReporting)
        }
    }
}
