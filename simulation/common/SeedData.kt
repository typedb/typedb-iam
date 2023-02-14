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
import com.vaticle.typedb.iam.simulation.common.concept.Company
import mu.KotlinLogging
import java.nio.file.Paths

class SeedData() {
    val adjectives = loadAdjectives()
    val applicationNames = loadApplicationNames()
    val applicationRoles = loadApplicationRoles()
    val businessUnitNames = loadBusinessUnitNames()
    val companyNames = loadCompanyNames()
    val femaleNames = loadFemaleNames()
    val fileExtensions = loadFileExtensions()
    val lastNames = loadLastNames()
    val maleNames = loadMaleNames()
    val nouns = loadNouns()
    val objectTypes = loadObjectTypes()
    val operationSets = loadOperationSets()
    val operations = loadOperations()
    val ownershipTypes = loadOwnershipTypes()
    val companies = initialiseCompanies(companyNames)

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private val ADJECTIVES_FILE = Paths.get("simulation/data/adjectives.yml")
        private val APPLICATION_NAMES_FILE = Paths.get("simulation/data/application-names.yml")
        private val APPLICATION_ROLES_FILE = Paths.get("simulation/data/application-roles.yml")
        private val BUSINESS_UNIT_NAMES_FILE = Paths.get("simulation/data/business-unit-names.yml")
        private val COMPANY_NAMES_FILE = Paths.get("simulation/data/company-names.yml")
        private val FEMALE_NAMES_FILE = Paths.get("simulation/data/female-names.yml")
        private val FILE_EXTENSIONS_FILE = Paths.get("simulation/data/file-extensions.yml")
        private val LAST_NAMES_FILE = Paths.get("simulation/data/last-names.yml")
        private val MALE_NAMES_FILE = Paths.get("simulation/data/male-names.yml")
        private val NOUNS_FILE = Paths.get("simulation/data/nouns.yml")
        private val OBJECT_TYPES_FILE = Paths.get("simulation/data/object-types.yml")
        private val OPERATION_SETS_FILE = Paths.get("simulation/data/operation-sets.yml")
        private val OPERATIONS_FILE = Paths.get("simulation/data/operations.yml")
        private val OWNERSHIP_TYPES_FILE = Paths.get("simulation/data/ownership-types.yml")

        private fun loadAdjectives(): List<String> {
            val yaml = YAML.load(ADJECTIVES_FILE)
            return yaml.asList().content().map { it.asMap().content()["value"]!!.asString().value() }
        }

        private fun loadApplicationNames(): List<String> {
            val yaml = YAML.load(APPLICATION_NAMES_FILE)
            return yaml.asList().content().map { it.asMap().content()["value"]!!.asString().value() }
        }

        private fun loadApplicationRoles(): List<String> {
            val yaml = YAML.load(APPLICATION_ROLES_FILE)
            return yaml.asList().content().map { it.asMap().content()["value"]!!.asString().value() }
        }

        private fun loadBusinessUnitNames(): List<String> {
            val yaml = YAML.load(BUSINESS_UNIT_NAMES_FILE)
            return yaml.asList().content().map { it.asMap().content()["value"]!!.asString().value() }
        }

        private fun loadCompanyNames(): List<Map<String, Any>> {
            val yaml = YAML.load(COMPANY_NAMES_FILE)

            val companyNames = yaml.asList().content().map { mapOf<String, Any>(
                "value" to it.asMap().content()["value"]!!.asString().value(),
                "rank" to it.asMap().content()["rank"]!!.asInt().value()
            ) }

            return companyNames
        }

        private fun loadFemaleNames(): List<Map<String, Any>> {
            val yaml = YAML.load(FEMALE_NAMES_FILE)

            val femaleNames = yaml.asList().content().map { mapOf<String, Any>(
                "value" to it.asMap().content()["value"]!!.asString().value(),
                "rank" to it.asMap().content()["rank"]!!.asInt().value(),
                "percentage" to it.asMap().content()["percentage"]!!.asFloat().value(),
                "percentile" to it.asMap().content()["percentile"]!!.asFloat().value()
            ) }

            return femaleNames
        }

        private fun loadFileExtensions(): List<String> {
            val yaml = YAML.load(FILE_EXTENSIONS_FILE)
            return yaml.asList().content().map { it.asMap().content()["value"]!!.asString().value() }
        }

        private fun loadLastNames(): List<Map<String, Any>> {
            val yaml = YAML.load(LAST_NAMES_FILE)

            val lastNames = yaml.asList().content().map { mapOf<String, Any>(
                "value" to it.asMap().content()["value"]!!.asString().value(),
                "rank" to it.asMap().content()["rank"]!!.asInt().value(),
                "percentage" to it.asMap().content()["percentage"]!!.asFloat().value(),
                "percentile" to it.asMap().content()["percentile"]!!.asFloat().value()
            ) }

            return lastNames
        }

        private fun loadMaleNames(): List<Map<String, Any>> {
            val yaml = YAML.load(MALE_NAMES_FILE)

            val maleNames = yaml.asList().content().map { mapOf<String, Any>(
                "value" to it.asMap().content()["value"]!!.asString().value(),
                "rank" to it.asMap().content()["rank"]!!.asInt().value(),
                "percentage" to it.asMap().content()["percentage"]!!.asFloat().value(),
                "percentile" to it.asMap().content()["percentile"]!!.asFloat().value()
            ) }

            return maleNames
        }

        private fun loadNouns(): List<String> {
            val yaml = YAML.load(NOUNS_FILE)
            return yaml.asList().content().map { it.asMap().content()["value"]!!.asString().value() }
        }

        private fun loadObjectTypes(): List<String> {
            val yaml = YAML.load(OBJECT_TYPES_FILE)
            return yaml.asList().content().map { it.asMap().content()["value"]!!.asString().value() }
        }

        private fun loadOperationSets(): List<Map<String, Any>> {
            val yaml = YAML.load(OPERATION_SETS_FILE)

            val operationSets = yaml.asList().content().map { operationSet -> mapOf<String, Any>(
                "value" to operationSet.asMap().content()["value"]!!.asString().value(),
                "objectTypes" to operationSet.asMap().content()["objectTypes"]!!.asList().content().map { it.asString().value() },
                "setMembers" to operationSet.asMap().content()["setMembers"]!!.asList().content().map { it.asString().value() }
            ) }

            return operationSets
        }

        private fun loadOperations(): List<Map<String, Any>> {
            val yaml = YAML.load(OPERATIONS_FILE)

            val operations = yaml.asList().content().map { operation -> mapOf<String, Any>(
                "value" to operation.asMap().content()["value"]!!.asString().value(),
                "objectTypes" to operation.asMap().content()["objectTypes"]!!.asList().content().map { it.asString().value() }
            ) }

            return operations
        }

        private fun loadOwnershipTypes(): List<String> {
            val yaml = YAML.load(OWNERSHIP_TYPES_FILE)
            return yaml.asList().content().map { it.asMap().content()["value"]!!.asString().value() }
        }

        private fun initialiseCompanies(companyNames: List<Map<String, Any>>): List<Company> {
            return companyNames.map { Company(code = it["rank"].toString(), name = it["value"].toString()) }
        }
    }
}
