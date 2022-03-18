package com.twosixlabs.dart.corpex.services.aggregation

package object exceptions {

    class QueryValidationException( val param : String, val value : String, val format : String ) extends Exception( s"${param}=${value} does not correspond to expected format: ${format}" )

}

