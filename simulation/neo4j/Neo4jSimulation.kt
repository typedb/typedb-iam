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
package com.vaticle.typedb.iam.simulation.neo4j

import com.vaticle.typedb.iam.simulation.agent.UserAgent
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.neo4j.agent.Neo4jAgentFactory
import com.vaticle.typedb.benchmark.framework.common.seed.RandomSource
import com.vaticle.typedb.benchmark.framework.neo4j.Neo4jClient
import mu.KotlinLogging
import org.neo4j.driver.Driver
import org.neo4j.driver.Session

class Neo4jSimulation private constructor(client: Neo4jClient, context: Context)
    : com.vaticle.typedb.benchmark.framework.neo4j.Neo4jSimulation<Context>(client, context, Neo4jAgentFactory(client, context)) {

    override val agentPackage: String = UserAgent::class.java.packageName

    override val name = "IAM"
//
//    /**
//     * Neo4j Community can create only uniqueness constraints, and only on nodes, not relationships. This means that it
//     * does not enforce the existence of a property on those nodes. `exists()` is only available in Neo4j Enterprise.
//     * https://neo4j.com/developer/kb/how-to-implement-a-primary-key-property-for-a-label/
//     *
//     * @param session
//     */
//    override fun addKeyConstraints(session: Session) {
//        val queries: List<String> = listOf(
//            "$CREATE $CONSTRAINT unique_person_email $ON ($PERSON:$PERSON_LABEL) $ASSERT $PERSON.$EMAIL $IS_UNIQUE",
//            "$CREATE $CONSTRAINT unique_continent_code $ON ($CONTINENT:$CONTINENT_LABEL) $ASSERT $CONTINENT.$CODE $IS_UNIQUE",
//            "$CREATE $CONSTRAINT unique_country_code $ON ($COUNTRY:$COUNTRY_LABEL) $ASSERT $COUNTRY.$CODE $IS_UNIQUE",
//            "$CREATE $CONSTRAINT unique_city_code $ON ($CITY:$CITY_LABEL) $ASSERT $CITY.$CODE $IS_UNIQUE",
//            "$CREATE $CONSTRAINT unique_company_number $ON ($COMPANY:$COMPANY_LABEL) $ASSERT $COMPANY.$NUMBER $IS_UNIQUE",
//            "$CREATE $CONSTRAINT unique_product_id $ON ($PRODUCT:$PRODUCT_LABEL) $ASSERT $PRODUCT.$ID $IS_UNIQUE",
//            "$CREATE $CONSTRAINT unique_purchase_id $ON ($PURCHASE:$PURCHASE_LABEL) $ASSERT $PURCHASE.$ID $IS_UNIQUE",
//            "$CREATE $CONSTRAINT unique_marriage_licence $ON ($MARRIAGE:$MARRIAGE_LABEL) $ASSERT $MARRIAGE.$LICENCE $IS_UNIQUE",
//        )
//        val tx = session.beginTransaction()
//        queries.forEach { tx.run(Query(it)) }
//        tx.commit()
//    }
//
//    override fun initData(nativeDriver: Driver) {
//        LOGGER.info("Neo4j initialisation of world simulation data started ...")
//        val start = Instant.now()
//        initContinents(nativeDriver, context.seedData.global)
//        LOGGER.info("Neo4j initialisation of world simulation data ended in: {}", printDuration(start, Instant.now()))
//    }
//
//    private fun initContinents(nativeDriver: Driver, global: Global) {
//        global.continents.parallelStream().forEach { continent: Continent ->
//            nativeDriver.session().use { session ->
//                val tx = session.beginTransaction()
//                tx.run(Query("$CREATE (x:$CONTINENT_LABEL:$REGION_LABEL {$CODE: '${continent.code}', $NAME: '${escapeQuotes(continent.name)}'})"))
//                tx.commit()
//                initCountries(nativeDriver, continent)
//            }
//        }
//    }
//
//    private fun initCountries(nativeDriver: Driver, continent: Continent) {
//        continent.countries.parallelStream().forEach { country: Country ->
//            nativeDriver.session().use { session ->
//                val tx = session.beginTransaction()
//                val currencyProps = StringBuilder()
//                if (country.currencies.isNotEmpty()) {
//                    currencyProps.append(", ")
//                    for (i in country.currencies.indices) {
//                        val currency = country.currencies[i]
//                        currencyProps.append(CURRENCY).append(i + 1).append(": '").append(currency.code).append("'")
//                        if (i + 1 < country.currencies.size) currencyProps.append(", ")
//                    }
//                }
//                val query = Query(
//                    "$MATCH (c:$CONTINENT_LABEL {$CODE: '${continent.code}'}) " +
//                            "$CREATE (x:$COUNTRY_LABEL:$REGION_LABEL {$CODE: '${country.code}', $NAME: '${escapeQuotes(country.name)}'$currencyProps})-[:$CONTAINED_IN]->(c)"
//                )
//                tx.run(query)
//                tx.commit()
//                initCities(session, country)
//                initUniversities(session, country)
//            }
//        }
//    }
//
//    private fun initCities(session: Session, country: Country) {
//        val tx = session.beginTransaction()
//        country.cities.forEach { city: City ->
//            val query = Query(
//                "$MATCH (c:$COUNTRY_LABEL {$CODE: '${country.code}'}) " +
//                        "$CREATE (x:$CITY_LABEL:$REGION_LABEL {$CODE: '${city.code}', $NAME: '${escapeQuotes(city.name)}'})-[:$CONTAINED_IN]->(c)",
//            )
//            tx.run(query)
//        }
//        tx.commit()
//    }
//
//    private fun initUniversities(session: Session, country: Country) {
//        val tx = session.beginTransaction()
//        country.universities.forEach { university: University ->
//            val query = Query(
//                "$MATCH (c:$COUNTRY_LABEL {$CODE: '${country.code}'}) $CREATE (x:$UNIVERSITY_LABEL {$NAME: '${escapeQuotes(university.name)}'})-[:$LOCATED_IN]->(c)",
//            )
//            tx.run(query)
//        }
//        tx.commit()
//    }
//
    companion object {
        private val LOGGER = KotlinLogging.logger {}

        fun create(hostUri: String, context: Context): Neo4jSimulation {
            return Neo4jSimulation(Neo4jClient(hostUri), context).apply { init() }
        }
    }
    override fun addKeyConstraints(session: Session) {
        TODO("Not yet implemented")
    }

    override fun initData(nativeDriver: Driver, randomSource: RandomSource) {
        TODO("Not yet implemented")
    }
}
