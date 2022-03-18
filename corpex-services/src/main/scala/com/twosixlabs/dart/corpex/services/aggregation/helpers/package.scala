package com.twosixlabs.dart.corpex.services.aggregation

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import com.fasterxml.jackson.databind.{JsonMappingException, ObjectMapper}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twosixlabs.dart.corpex.services.aggregation.exceptions.QueryValidationException
import com.twosixlabs.dart.corpex.services.aggregation.models.AggregationQuery
import com.twosixlabs.dart.corpex.services.aggregation.CdrService
import com.twosixlabs.dart.exceptions.{BadQueryParameterException, BadRequestBodyException}
import com.twosixlabs.dart.rest.scalatra.models.HealthStatus
import com.twosixlabs.dart.utils.DatesAndTimes

import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import scala.util.{Failure, Success, Try}

package object helpers {

    private val m : ObjectMapper = {
        val mapper = new ObjectMapper()
        mapper registerModule ( DefaultScalaModule )
        mapper registerModule ( new JavaTimeModule )
        mapper
    }

    def unmarshal[ A ]( json : String, valueType : Class[ A ] ) : A = m.readValue( json, valueType )

    def marshal( dto : Any ) : String = m.writeValueAsString( dto )

    def unmarshalQueryRequest( json : String ) : AggregationQuery = {
        Try( m.readValue( json, classOf[ AggregationQuery] ) ) match {
            case Success( query : AggregationQuery ) => query
            case Failure( e : JsonMappingException ) => throw new BadRequestBodyException( s"Unable to parse request body as a query definition: ${e.getMessage}" )
            case Failure( e : JsonParseException ) => throw new BadRequestBodyException( s"Unable to parse request body as json: ${e.getMessage}" )
            case Failure( e : QueryValidationException ) => throw new BadRequestBodyException( s"field ${e.getMessage}" )
            case Failure( e : InvalidDefinitionException ) => throw new BadRequestBodyException( s"field ${e.getMessage.stripPrefix( "Cannot construct instance of `com.twosixlabs.dart.cdr.aggregation.models.AggregationQuery`, problem: " )}" )
            case Failure( e : Throwable ) => throw e
        }
    }

    def parseDate( dateStr : String ) : Long = {
        Try {
            val dateTime = DatesAndTimes.fromIsoLocalDateStr( dateStr )
              .atTime( 23, 59, 59 )
              .atOffset( ZoneOffset.UTC )

            dateTime.toEpochSecond
        } match {
            case Success( dt : Long ) => dt
            case Failure( e : DateTimeParseException ) =>
                throw new BadQueryParameterException( "date", Some( dateStr ), "YYYY-MM-DD" )
            case Failure( e : Throwable ) => throw e
        }
    }

    def checkHealth( cdrService: CdrService ) : (HealthStatus.HealthStatus, Option[ String ]) = {
        cdrService.checkStatus match {
            case (true, _) => (HealthStatus.HEALTHY, None)
            case (false, msg : Option[ String ]) => (HealthStatus.UNHEALTHY, msg)
        }
    }

}
