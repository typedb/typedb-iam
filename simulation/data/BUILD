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

load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

filegroup(
    name = "data",
    srcs = [
        "adjectives.yml",
        "applications.yml",
        "roles.yml",
        "business-units.yml",
        "companies.yml",
        "female-names.yml",
        "file-extensions.yml",
        "last-names.yml",
        "male-names.yml",
        "nouns.yml",
        "operation-sets.yml",
        "operations.yml",
        "ownership-types.yml"
    ],
    visibility = ["//visibility:public"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    exclude = glob(["*.yml"]),
    license_type = "agpl-header",
)
