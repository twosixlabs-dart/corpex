package com.twosixlabs.dart.corpex.api

package object exceptions {

    class CorpexEnumException( value: String, enumName: String ) extends Exception( s"${value} does not map to any value of enum ${enumName}" )

    class InvalidRequestException( problem : String ) extends Exception( s"invalid request: ${ problem }" )

    class InvalidSearchQueryException( problem : String ) extends Exception( s"invalid search query: ${ problem }" )

    class InvalidAggQueryException( problem : String ) extends Exception( s"invalid aggregation query: ${ problem }" )

}
