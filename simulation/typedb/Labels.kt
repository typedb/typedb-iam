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
package com.vaticle.typedb.iam.simulation.typedb

object Labels {
    const val COMPANY = "company"
    const val COMPANY_MEMBERSHIP = "company-membership"
    const val COMPANY_MEMBER = "company-member"
    const val PARENT_COMPANY = "parent-company"
    const val SUBJECT = "subject"
    const val USER = "user"
    const val USER_GROUP = "user-group"
    const val OBJECT = "object"
    const val RESOURCE = "resource"
    const val RESOURCE_COLLECTION = "resource-collection"
    const val ACTION = "action"
    const val OPERATION = "operation"
    const val OPERATION_SET = "operation-set"
    const val GROUP_MEMBERSHIP = "group-membership"
    const val GROUP_MEMBER = "group-member"
    const val COLLECTION_MEMBERSHIP = "collection-membership"
    const val COLLECTION_MEMBER = "collection-member"
    const val SET_MEMBERSHIP = "set-membership"
    const val SET_MEMBER = "set-member"
    const val GROUP_OWNERSHIP = "group-ownership"
    const val OWNED_GROUP = "owned-group"
    const val GROUP_OWNER = "group-owner"
    const val OBJECT_OWNERSHIP = "object-ownership"
    const val OWNED_OBJECT = "owned-object"
    const val OBJECT_OWNER = "object-owner"
    const val ACCESS = "access"
    const val ACCESSED_OBJECT = "accessed-object"
    const val VALID_ACTION = "valid-action"
    const val PERMISSION = "permission"
    const val PERMITTED_SUBJECT = "permitted-subject"
    const val PERMITTED_ACCESS = "permitted-access"
    const val CHANGE_REQUEST = "change-request"
    const val REQUESTING_SUBJECT = "requesting-subject"
    const val REQUESTED_SUBJECT = "requested-subject"
    const val REQUESTED_CHANGE = "requested-change"
    const val SEGREGATION_POLICY = "segregation-policy"
    const val SEGREGATED_ACTION = "segregated-action"
    const val SEGREGATION_VIOLATION = "segregation-violation"
    const val VIOLATING_SUBJECT = "violating-subject"
    const val VIOLATING_OBJECT = "violating-object"
    const val VIOLATING_POLICY = "violated-policy"
    const val CREDENTIAL = "credential"
    const val OBJECT_TYPE = "object-type"
    const val NAME = "name"
    const val OWNERSHIP_TYPE = "ownership-type"
    const val REVIEW_DATE = "review-date"
    const val VALIDITY = "validity"
    const val PERSON = "person"
    const val BUSINESS_UNIT = "business-unit"
    const val USER_ROLE = "user-role"
    const val USER_ACCOUNT = "user-account"
    const val FILE = "file"
    const val INTERFACE = "interface"
    const val RECORD = "record"
    const val DIRECTORY = "directory"
    const val APPLICATION = "application"
    const val DATABASE = "database"
    const val TABLE = "table"
    const val EMAIL = "email"
    const val FILEPATH = "filepath"
    const val NUMBER = "number"
}