package com.vaticle.typedb.iam.simulation.typedb.agent

import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.READ
import com.vaticle.typedb.client.api.TypeDBTransaction.Type.WRITE
import com.vaticle.typedb.iam.simulation.common.Context
import com.vaticle.typedb.iam.simulation.agent.User
import com.vaticle.typedb.iam.simulation.common.SeedData
import com.vaticle.typedb.iam.simulation.common.concept.*
import com.vaticle.typedb.iam.simulation.typedb.Labels.ATTRIBUTE
import com.vaticle.typedb.iam.simulation.typedb.Labels.COLLECTION_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COLLECTION_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBER
import com.vaticle.typedb.iam.simulation.typedb.Labels.COMPANY_MEMBERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.DIRECTORY
import com.vaticle.typedb.iam.simulation.typedb.Labels.FILE
import com.vaticle.typedb.iam.simulation.typedb.Labels.NAME
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNER
import com.vaticle.typedb.iam.simulation.typedb.Labels.OBJECT_OWNERSHIP
import com.vaticle.typedb.iam.simulation.typedb.Labels.OWNED_OBJECT
import com.vaticle.typedb.iam.simulation.typedb.Labels.PATH
import com.vaticle.typedb.iam.simulation.typedb.Labels.PARENT_COMPANY
import com.vaticle.typedb.iam.simulation.typedb.Labels.RESOURCE_COLLECTION
import com.vaticle.typedb.iam.simulation.typedb.Labels.SUBJECT
import com.vaticle.typedb.simulation.common.seed.RandomSource
import com.vaticle.typedb.simulation.typedb.TypeDBClient
import com.vaticle.typeql.lang.TypeQL.*
import java.lang.IllegalArgumentException
import kotlin.streams.toList

class TypeDBUser(client: TypeDBClient, context:Context): User<TypeDBSession>(client, context) {
    override fun createObject(session: TypeDBSession, company: Company, randomSource: RandomSource): List<Report> {
        val objectType = randomSource.choose(context.seedData.objectTypes.filter { it != ObjectType.APPLICATION })

        when (objectType) {
            ObjectType.FILE -> createFile(session, company, context.seedData, randomSource)
            ObjectType.DIRECTORY -> createDirectory(session, company, context.seedData, randomSource)
            ObjectType.INTERFACE -> createInterface(session, company, context.seedData, randomSource)
            ObjectType.APPLICATION -> throw IllegalArgumentException() // Applications are only created on database initialisation.
            ObjectType.DATABASE -> createDatabase(session, company, context.seedData, randomSource)
            ObjectType.TABLE -> createTable(session, company, context.seedData, randomSource)
            ObjectType.RECORD -> createRecord(session, company, context.seedData, randomSource)
        }
    }

    override fun deleteObject(session: TypeDBSession, company: Company, seedData: SeedData, randomSource: RandomSource): List<Report> {
        TODO("Not yet implemented")
    }

    override fun attemptAccess(session: TypeDBSession, company: Company, seedData: SeedData, randomSource: RandomSource): List<Report> {
        TODO("Not yet implemented")
    }

    override fun submitChangeRequest(session: TypeDBSession, company: Company, seedData: SeedData, randomSource: RandomSource): List<Report> {
        TODO("Not yet implemented")
    }

    private fun createFile(session: TypeDBSession, company: Company, seedData: SeedData, randomSource: RandomSource) {
        val directories: List<Directory>

        session.transaction(READ).use { transaction ->
            directories = transaction.query().match(
                match(
                    `var`(D).isa(DIRECTORY).has(PARENT_COMPANY, company.name).has(PATH, D_PATH)
                )
            ).toList().map { Directory(it[D_PATH].asAttribute().asString().value) }
        }

        val directory = randomSource.choose(directories)
        val subjectIdentifiers: List<Attribute>

        session.transaction(READ).use { transaction ->
            subjectIdentifiers = transaction.query().match(
                match(
                    `var`(S).isa(SUBJECT).has(PARENT_COMPANY, company.name).has(ATTRIBUTE, S_ATTRIBUTE),
                    `var`(S_ATTRIBUTE).isa(S_ATTRIBUTE_TYPE)
                )
            ).toList().map { Attribute(it[S_ATTRIBUTE_TYPE].asType().label.name(), it[S_ATTRIBUTE].asAttribute().asString().value) }
        }

        val subjectIdentifier = randomSource.choose(subjectIdentifiers)
        val file = File.initialise(directory, seedData, randomSource)

        session.transaction(WRITE).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(C).isa(COMPANY).has(NAME, company.name),
                    `var`(D).isa(DIRECTORY).has(PARENT_COMPANY, company.name).has(PATH, directory.path),
                    `var`(S).isa(SUBJECT).has(PARENT_COMPANY, company.name).has(subjectIdentifier.label, subjectIdentifier.value)
                ).insert(
                    `var`(F).isa(FILE).has(PATH, file.path),
                    rel(COMPANY, C).rel(COMPANY_MEMBER, F).isa(COMPANY_MEMBERSHIP),
                    rel(RESOURCE_COLLECTION, D).rel(COLLECTION_MEMBER, F).isa(COLLECTION_MEMBERSHIP),
                    rel(OWNED_OBJECT, F).rel(OBJECT_OWNER, S).isa(OBJECT_OWNERSHIP)
                )
            )
        }
    }

    private fun createDirectory(session: TypeDBSession, company: Company, seedData: SeedData, randomSource: RandomSource) {
        val directories: List<Directory>

        session.transaction(READ).use { transaction ->
            directories = transaction.query().match(
                match(
                    `var`(D).isa(DIRECTORY).has(PARENT_COMPANY, company.name).has(PATH, D_PATH)
                )
            ).toList().map { Directory(it[D_PATH].asAttribute().asString().value) }
        }

        val parent = randomSource.choose(directories)
        val subjectIdentifiers: List<Attribute>

        session.transaction(READ).use { transaction ->
            subjectIdentifiers = transaction.query().match(
                match(
                    `var`(S).isa(SUBJECT).has(PARENT_COMPANY, company.name).has(ATTRIBUTE, S_ATTRIBUTE),
                    `var`(S_ATTRIBUTE).isa(S_ATTRIBUTE_TYPE)
                )
            ).toList().map { Attribute(it[S_ATTRIBUTE_TYPE].asType().label.name(), it[S_ATTRIBUTE].asAttribute().asString().value) }
        }

        val subjectIdentifier = randomSource.choose(subjectIdentifiers)
        val file = Directory.initialise(parent, seedData, randomSource)

        session.transaction(WRITE).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(C).isa(COMPANY).has(NAME, company.name),
                    `var`(PD).isa(DIRECTORY).has(PARENT_COMPANY, company.name).has(PATH, parent.path),
                    `var`(S).isa(SUBJECT).has(PARENT_COMPANY, company.name).has(subjectIdentifier.label, subjectIdentifier.value)
                ).insert(
                    `var`(D).isa(DIRECTORY).has(PATH, file.path),
                    rel(COMPANY, C).rel(COMPANY_MEMBER, F).isa(COMPANY_MEMBERSHIP),
                    rel(RESOURCE_COLLECTION, D).rel(COLLECTION_MEMBER, F).isa(COLLECTION_MEMBERSHIP),
                    rel(OWNED_OBJECT, F).rel(OBJECT_OWNER, S).isa(OBJECT_OWNERSHIP)
                )
            )
        }
    }

    private fun createInterface(session: TypeDBSession, company: Company, seedData: SeedData, randomSource: RandomSource) {
        val directories: List<Directory>

        session.transaction(READ).use { transaction ->
            directories = transaction.query().match(
                match(
                    `var`(D).isa(DIRECTORY).has(PARENT_COMPANY, company.name).has(PATH, D_PATH)
                )
            ).toList().map { Directory(it[D_PATH].asAttribute().asString().value) }
        }

        val directory = randomSource.choose(directories)
        val subjectIdentifiers: List<Attribute>

        session.transaction(READ).use { transaction ->
            subjectIdentifiers = transaction.query().match(
                match(
                    `var`(S).isa(SUBJECT).has(PARENT_COMPANY, company.name).has(ATTRIBUTE, S_ATTRIBUTE),
                    `var`(S_ATTRIBUTE).isa(S_ATTRIBUTE_TYPE)
                )
            ).toList().map { Attribute(it[S_ATTRIBUTE_TYPE].asType().label.name(), it[S_ATTRIBUTE].asAttribute().asString().value) }
        }

        val subjectIdentifier = randomSource.choose(subjectIdentifiers)
        val file = File.initialise(directory, seedData, randomSource)

        session.transaction(WRITE).use { transaction ->
            transaction.query().insert(
                match(
                    `var`(C).isa(COMPANY).has(NAME, company.name),
                    `var`(D).isa(DIRECTORY).has(PARENT_COMPANY, company.name).has(PATH, directory.path),
                    `var`(S).isa(SUBJECT).has(PARENT_COMPANY, company.name).has(subjectIdentifier.label, subjectIdentifier.value)
                ).insert(
                    `var`(F).isa(FILE).has(PATH, file.path),
                    rel(COMPANY, C).rel(COMPANY_MEMBER, F).isa(COMPANY_MEMBERSHIP),
                    rel(RESOURCE_COLLECTION, D).rel(COLLECTION_MEMBER, F).isa(COLLECTION_MEMBERSHIP),
                    rel(OWNED_OBJECT, F).rel(OBJECT_OWNER, S).isa(OBJECT_OWNERSHIP)
                )
            )
        }
    }

    private fun createDatabase(session: TypeDBSession, company: Company, seedData: SeedData, randomSource: RandomSource) {}
    private fun createTable(session: TypeDBSession, company: Company, seedData: SeedData, randomSource: RandomSource) {}
    private fun createRecord(session: TypeDBSession, company: Company, seedData: SeedData, randomSource: RandomSource) {}

    companion object {
        private const val A = "a"
        private const val B = "b"
        private const val C = "c"
        private const val D = "d"
        private const val D_PATH = "d-path"
        private const val F = "f"
        private const val O = "o"
        private const val P = "p"
        private const val PD = "pd"
        private const val P_EMAIL = "p-email"
        private const val P_NAME = "p-name"
        private const val P_IDENTIFIER = "p-identifier"
        private const val R = "r"
        private const val S = "s"
        private const val S_ATTRIBUTE = "s-attribute"
        private const val S_ATTRIBUTE_TYPE = "s-attribute-type"
        private const val ROOT = "root"
    }
}
