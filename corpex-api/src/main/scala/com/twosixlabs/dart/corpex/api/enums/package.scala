package com.twosixlabs.dart.corpex.api

import com.fasterxml.jackson.core.`type`.TypeReference
import com.twosixlabs.dart.corpex.api.exceptions.CorpexEnumException

package object enums {

    object BoolType extends Enumeration {
        type BoolType = Value
        val MUST, MUST_NOT, SHOULD, FILTER = Value
    }

    class BoolTypeType extends TypeReference[ BoolType.type ]

    object QueryType extends Enumeration {
        type QueryType = Value
        val TEXT, TERM, INTEGER, CDR_DATE, TAG_DATE, TAG, FACET, BOOL = Value
    }

    class QueryTypeType extends TypeReference[ QueryType.type ]

    object AggType extends Enumeration {
        type AggType = Value
        val FACET, FACET_CONFIDENCE, TAG_TYPES, TAG_VALUES, FIELD = Value
    }

    class AggTypeType extends TypeReference[ AggType.type ]

    object SortType extends Enumeration {
        type SortType = Value
        val ASC = Value( "asc" )
        val DESC = Value( "desc" )
    }

    class SortTypeType extends TypeReference[ SortType.type ]

    // The following enums are not used for deserialization, and thus do not require TypeReference companion classes
    object SortValues extends Enumeration {
        type SortValues = Value
        val VALUES = Value( "values" )
        val NUM_DOCS = Value( "numDocs" )
    }

}
