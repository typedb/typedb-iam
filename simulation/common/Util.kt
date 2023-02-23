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

import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.common.yaml.YAML
import java.time.Duration
import java.time.Instant

object Util {
    fun printDuration(start: Instant, end: Instant): String {
        return Duration.between(start, end).toString()
            .substring(2)
            .replace("(\\d[HMS])(?!$)".toRegex(), "$1 ")
            .lowercase()
    }

    fun int(yaml: YAML?): Int = yaml!!.asInt().value()
    fun float(yaml: YAML?): Float = yaml!!.asFloat().value()
    fun string(yaml: YAML?): String = yaml!!.asString().value()
    fun map(yaml: YAML?): Map<String, YAML> = yaml!!.asMap().content()
    fun list(yaml: YAML?): List<YAML> = yaml!!.asList().content()

    fun typeLabel(typeConcept: Concept): String {
        assert(typeConcept.isType)
        return typeConcept.asType().label.name()
    }

    fun booleanValue(attributeConcept: Concept): Boolean {
        assert(attributeConcept.isAttribute)
        return attributeConcept.asAttribute().asBoolean().value
    }

    fun longValue(attributeConcept: Concept): Long {
        assert(attributeConcept.isAttribute)
        return attributeConcept.asAttribute().asLong().value
    }

    fun stringValue(attributeConcept: Concept): String {
        assert(attributeConcept.isAttribute)
        return attributeConcept.asAttribute().asString().value
    }
}
