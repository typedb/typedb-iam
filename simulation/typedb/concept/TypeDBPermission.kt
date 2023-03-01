package com.vaticle.typedb.iam.simulation.typedb.concept

import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.iam.simulation.typedb.Util.booleanValue
import com.vaticle.typedb.iam.simulation.typedb.Util.datetimeValue
import com.vaticle.typedb.iam.simulation.typedb.Util.longValue
import java.time.LocalDateTime

data class TypeDBPermission(val permittedSubject: TypeDBSubject, val permittedAccess: TypeDBAccess, val validity: Boolean, val reviewDate: LocalDateTime) {
    constructor(permittedSubject: TypeDBSubject, permittedAccess: TypeDBAccess, validity: Concept, reviewDate: Concept):
            this(permittedSubject, permittedAccess, booleanValue(validity), datetimeValue(reviewDate))
}