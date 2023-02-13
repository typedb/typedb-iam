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
import com.vaticle.typedb.iam.simulation.common.concept.City
import com.vaticle.typedb.iam.simulation.common.concept.Continent
import com.vaticle.typedb.iam.simulation.common.concept.Country
import com.vaticle.typedb.iam.simulation.common.concept.Currency
import com.vaticle.typedb.iam.simulation.common.concept.Global
import com.vaticle.typedb.iam.simulation.common.concept.University
import com.vaticle.typedb.simulation.common.Util.parse
import com.vaticle.typedb.simulation.common.Util.readPair
import com.vaticle.typedb.simulation.common.Util.readSingle
import com.vaticle.typedb.simulation.common.Util.readTriple
import mu.KotlinLogging
import java.nio.file.Paths

class SeedData(val global: Global) {
    val continents get() = global.continents

    val countries get(): List<Country> {
        return continents.flatMap { it.countries }
    }

    val cities get(): List<City> {
        return countries.flatMap { it.cities }
    }

    val universities get(): List<University> {
        return countries.flatMap { it.universities }
    }

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

        fun initialise(): SeedData {
            val global = Global()
            val continents = mutableMapOf<String, Continent>()
            val countries = mutableMapOf<String, Country>()
            initialiseContinents(global, continents)
            initialiseCountries(continents, countries)
            initialiseCurrencies(countries)
            initialiseCities(countries)
            initialiseUniversities(countries)
            initialiseLastNames(continents)
            initialiseFemaleFirstNames(continents)
            initialiseMaleFirstNames(continents)
            initialiseAdjectives(words)
            initialiseNouns(words)
            prune(global)
            return SeedData(global)
        }

        private fun initialiseAdjectives() {
            val adjectives = YAML.load(ADJECTIVES_FILE)
        }

        private fun initialiseApplicationNames() {
            words.adjectives += parse(ADJECTIVES_FILE).map { it.readSingle() }
        }

        private fun initialiseContinents(global: Global, continents: MutableMap<String, Continent>) {
            parse(CONTINENTS_FILE).map { it.readPair() }.forEach { (code, name) ->
                val continent = Continent(code, name)
                global.continents += continent
                continents[code] = continent
            }
        }

        private fun initialiseCountries(continents: Map<String, Continent>, countries: MutableMap<String, Country>) {
            parse(COUNTRIES_FILE).map { it.readTriple() }.forEach { (code, name, continentCode) ->
                val continent = requireNotNull(continents[continentCode])
                val country = Country(code, name, continent)
                continent.countries += country
                countries[code] = country
            }
        }

        private fun initialiseCurrencies(countries: Map<String, Country>) {
            val currencies = mutableMapOf<String, Currency>()
            parse(CURRENCIES_FILE).map { it.readTriple() }.forEach { (code, name, countryCode) ->
                val country = requireNotNull(countries[countryCode])
                val currency = currencies.computeIfAbsent(code) { Currency(it, name) }
                country.currencies += currency
            }
        }

        private fun initialiseCities(countries: Map<String, Country>) {
            parse(CITIES_FILE).map { it.readTriple() }.forEach { (code, name, countryCode) ->
                val country = requireNotNull(countries[countryCode])
                val city = City(code, name, country)
                country.cities += city
            }
        }

        private fun initialiseUniversities(countries: Map<String, Country>) {
            parse(UNIVERSITIES_FILE).map { it.readPair() }.forEach { (name, countryCode) ->
                val country = requireNotNull(countries[countryCode])
                val university = University(name, country)
                country.universities += university
            }
        }

        private fun initialiseLastNames(continents: Map<String, Continent>) {
            parse(LAST_NAMES_FILE).map { it.readPair() }.forEach { (name, continentCode) ->
                val continent = requireNotNull(continents[continentCode])
                continent.commonLastNames += name
            }
        }

        private fun initialiseFemaleFirstNames(continents: Map<String, Continent>) {
            parse(FIRST_NAMES_FEMALE_FILE).map { it.readPair() }.forEach { (name, continentCode) ->
                val continent = requireNotNull(continents[continentCode])
                continent.commonFemaleFirstNames += name
            }
        }

        private fun initialiseMaleFirstNames(continents: Map<String, Continent>) {
            parse(FIRST_NAMES_MALE_FILE).map { it.readPair() }.forEach { (name, continentCode) ->
                val continent = requireNotNull(continents[continentCode])
                continent.commonMaleFirstNames += name
            }
        }

        private fun initialiseNouns(words: Words) {
            words.nouns += parse(NOUNS_FILE).map { it.readSingle() }
        }
    }
}
