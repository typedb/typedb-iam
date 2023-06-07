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
package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.iam.simulation.typedb.Labels as Labels

enum class TypeDBObjectType(val label: String, val type: String, val generable: Boolean) {
    FILE(Labels.FILE, Labels.RESOURCE, true),
    DIRECTORY(Labels.DIRECTORY, Labels.RESOURCE_COLLECTION, true),
    INTERFACE(Labels.INTERFACE, Labels.RESOURCE, true),
    APPLICATION(Labels.APPLICATION, Labels.RESOURCE_COLLECTION, false),
    RECORD(Labels.RECORD, Labels.RESOURCE, true),
    TABLE(Labels.TABLE, Labels.RESOURCE_COLLECTION, true),
    DATABASE(Labels.DATABASE, Labels.RESOURCE_COLLECTION, true)
}