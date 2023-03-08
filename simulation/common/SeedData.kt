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
import com.vaticle.typedb.iam.simulation.common.Util.int
import com.vaticle.typedb.iam.simulation.common.Util.double
import com.vaticle.typedb.iam.simulation.common.Util.string
import com.vaticle.typedb.iam.simulation.common.Util.map
import com.vaticle.typedb.iam.simulation.common.Util.list
import com.vaticle.typedb.iam.simulation.common.concept.Application
import com.vaticle.typedb.iam.simulation.common.concept.BusinessUnit
import com.vaticle.typedb.iam.simulation.common.concept.Company
import com.vaticle.typedb.iam.simulation.common.concept.Operation
import com.vaticle.typedb.iam.simulation.common.concept.OperationSet
import com.vaticle.typedb.iam.simulation.common.concept.UserRole
import mu.KotlinLogging
import java.nio.file.Paths

class SeedData(config: Config) {
    val adjectives = loadAdjectives()
    val applications = initialiseApplications()
    val businessUnits = initialiseBusinessUnits()
    val companies = initialiseCompanies(config)
    val femaleNames = loadFemaleNames()
    val fileExtensions = loadFileExtensions()
    val lastNames = loadLastNames()
    val maleNames = loadMaleNames()
    val nouns = loadNouns()
    val operationSets = initialiseOperationSets()
    val operations = initialiseOperations()
    val ownershipTypes = loadOwnershipTypes()
    val userRoles = initialiseUserRoles()
    
    

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private val ADJECTIVES_FILE = Paths.get("simulation/data/adjectives.yml")
        private val APPLICATIONS_FILE = Paths.get("simulation/data/applications.yml")
        private val BUSINESS_UNITS_FILE = Paths.get("simulation/data/business-units.yml")
        private val COMPANIES_FILE = Paths.get("simulation/data/companies.yml")
        private val FEMALE_NAMES_FILE = Paths.get("simulation/data/female-names.yml")
        private val FILE_EXTENSIONS_FILE = Paths.get("simulation/data/file-extensions.yml")
        private val LAST_NAMES_FILE = Paths.get("simulation/data/last-names.yml")
        private val MALE_NAMES_FILE = Paths.get("simulation/data/male-names.yml")
        private val NOUNS_FILE = Paths.get("simulation/data/nouns.yml")
        private val OPERATION_SETS_FILE = Paths.get("simulation/data/operation-sets.yml")
        private val OPERATIONS_FILE = Paths.get("simulation/data/operations.yml")
        private val OWNERSHIP_TYPES_FILE = Paths.get("simulation/data/ownership-types.yml")
        private val ROLES_FILE = Paths.get("simulation/data/roles.yml")

        private fun loadAdjectives(): List<String> {
            val yaml = YAML.load(ADJECTIVES_FILE)
            return list(yaml).map { string(map(it)[VALUE]) }
        }

        private fun initialiseApplications(): List<Application> {
            val yaml = YAML.load(APPLICATIONS_FILE)
            return list(yaml).map { Application(string(map(it)[VALUE])) }
        }

        private fun initialiseBusinessUnits(): List<BusinessUnit> {
            val yaml = YAML.load(BUSINESS_UNITS_FILE)
            return list(yaml).map { BusinessUnit(string(map(it)[VALUE])) }
        }

        private fun initialiseCompanies(config: Config): List<Company> {
            val yaml = YAML.load(COMPANIES_FILE)

            val companies = list(yaml).map { Company(
                string(map(it)[VALUE]),
                int(map(it)[RANK])
            ) }

            if (config.run.partitions > companies.size) throw IllegalArgumentException("Partition count exceeds the number of supplied partitions.")
            else return companies.subList(0, config.run.partitions)
        }

        private fun loadFemaleNames(): List<Map<String, Any>> {
            val yaml = YAML.load(FEMALE_NAMES_FILE)

            val femaleNames = list(yaml).map { mapOf(
                VALUE to string(map(it)[VALUE]),
                RANK to int(map(it)[RANK]),
                PERCENTAGE to double(map(it)[PERCENTAGE]),
                PERCENTILE to double(map(it)[PERCENTILE])
            ) }

            return femaleNames
        }

        private fun loadFileExtensions(): List<String> {
            val yaml = YAML.load(FILE_EXTENSIONS_FILE)
            return list(yaml).map { string(map(it)[VALUE]) }
        }

        private fun loadLastNames(): List<Map<String, Any>> {
            val yaml = YAML.load(LAST_NAMES_FILE)

            val lastNames = list(yaml).map { mapOf(
                VALUE to string(map(it)[VALUE]),
                RANK to int(map(it)[RANK]),
                PERCENTAGE to double(map(it)[PERCENTAGE]),
                PERCENTILE to double(map(it)[PERCENTILE])
            ) }

            return lastNames
        }

        private fun loadMaleNames(): List<Map<String, Any>> {
            val yaml = YAML.load(MALE_NAMES_FILE)

            val maleNames = list(yaml).map { mapOf(
                VALUE to string(map(it)[VALUE]),
                RANK to int(map(it)[RANK]),
                PERCENTAGE to double(map(it)[PERCENTAGE]),
                PERCENTILE to double(map(it)[PERCENTILE])
            ) }

            return maleNames
        }

        private fun loadNouns(): List<String> {
            val yaml = YAML.load(NOUNS_FILE)
            return list(yaml).map { string(map(it)[VALUE]) }
        }

        private fun initialiseOperationSets(): List<OperationSet> {
            val yaml = YAML.load(OPERATION_SETS_FILE)

            return list(yaml).map { operationSet -> OperationSet(
                string(map(operationSet)[VALUE]),
                list(map(operationSet)[OBJECT_TYPES]).map { string(it) },
                list(map(operationSet)[SET_MEMBERS]).map { string(it) }
            ) }
        }

        private fun initialiseOperations(): List<Operation> {
            val yaml = YAML.load(OPERATIONS_FILE)
            
            return list(yaml).map { operation -> Operation(
                string(map(operation)[VALUE]),
                list(map(operation)[OBJECT_TYPES]).map { string(it) }
            ) }
        }

        private fun loadOwnershipTypes(): List<String> {
            val yaml = YAML.load(OWNERSHIP_TYPES_FILE)
            return list(yaml).map { string(map(it)[VALUE]) }
        }

        private fun initialiseUserRoles(): List<UserRole> {
            val yamlApplications = YAML.load(APPLICATIONS_FILE)
            val yamlRoles = YAML.load(ROLES_FILE)
            val applications = list(yamlApplications).map { string(map(it)[VALUE]) }
            val roles = list(yamlRoles).map { string(map(it)[VALUE]) }
            val userRoles: MutableList<String> = mutableListOf()

            applications.forEach { application ->
                roles.forEach { role ->
                    userRoles.add("$application $role")
                }
            }

            return userRoles.map { UserRole(it) }
        }

        private const val VALUE = "value"
        private const val RANK = "rank"
        private const val PERCENTAGE = "percentage"
        private const val PERCENTILE = "percentile"
        private const val OBJECT_TYPES = "objectTypes"
        private const val SET_MEMBERS = "setMembers"
    }
}
