#
# Copyright (C) 2022 Vaticle
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

filegroup(
    name = "data",
    srcs = [
        "adjectives.tsv",
        "application-names.tsv",
        "application-roles.tsv",
        "business-unit-names.tsv",
        "company-names.tsv",
        "female-names.tsv",
        "file-extensions.tsv",
        "last-names.tsv",
        "male-names.tsv",
        "nouns.tsv",
        "object-types.tsv",
        "operation-sets.tsv",
        "operations.tsv",
        "ownership-types.tsv"
    ],
    visibility = ["//visibility:public"],
)

kt_jvm_library(
    name = "neo4j",
    srcs = glob(["*.kt", "*/*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        # External Vaticle Dependencies
        "@vaticle_typedb_common//:common"
    ],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    exclude = glob(["*.csv"]),
    license_type = "agpl-header",
)
