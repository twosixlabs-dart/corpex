package com.twosixlabs.dart.corpex.services.aggregation

import com.twosixlabs.cdr4s.annotations.OffsetTag
import com.twosixlabs.cdr4s.core.{ CdrAnnotation, CdrDocument, CdrMetadata, OffsetTagAnnotation }
import com.twosixlabs.cdr4s.json.dart.{ DartJsonFormat, DartMetadataDto }
import com.twosixlabs.dart.corpex.api.models.{ CorpexSearchRequest }
import com.twosixlabs.dart.corpex.services.aggregation.exceptions.QueryValidationException
import com.twosixlabs.dart.corpex.services.aggregation.models.{ AggregationQuery, ValueCount }
import com.twosixlabs.dart.corpex.services.search.SearchService
import com.twosixlabs.dart.exceptions.{ BadQueryParameterException, ResourceNotFoundException, ServiceUnreachableException }
import com.twosixlabs.dart.json.JsonFormat
import org.slf4j.{ Logger, LoggerFactory }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future, TimeoutException }
import scala.util.{ Failure, Success, Try }

class CdrService( props : Map[ String, String ], docService : SearchService ) {

    val LOG : Logger = LoggerFactory.getLogger( getClass )
    lazy val jsonFormat = new DartJsonFormat

    val defaultMinRes = props.get( "aggregator.default.min.res" ).map( _.toInt )
    val defaultMaxRes = props.get( "aggregator.default.max.res" ).map( _.toInt )

    val tagIdMap = props.withFilter( _._1.startsWith( "api.labels.map.tag.id." ) ).map( tup => {
        val (key, value) = tup
        (key.stripPrefix( "api.labels.map.tag.id." ).trim.toLowerCase, value.trim)
    } )
    val tagTypeMap = props.withFilter( _._1.startsWith( "api.labels.map.tag.type." ) ).map( tup => {
        val (key, value) = tup
        (key.stripPrefix( "api.labels.map.tag.type." ).trim.toLowerCase, value.trim)
    } )

    def getAggregation( docId : String, query : AggregationQuery, date : Option[ String ] ) : List[ ValueCount ] = {
        import helpers._

        val cdrDto = docService.getDocument( docId, Some( "annotations" ) )

        val cdr = ( for {
            cdrString <- JsonFormat.marshalFrom( cdrDto ).toOption
            cdr <- jsonFormat.unmarshalCdr( cdrString )
        } yield cdr ).get

        aggregateCdr( cdr, query )
    }

    def aggregateCdr( cdr : CdrDocument, query : AggregationQuery ) : List[ ValueCount ] = {
        val minRes = if (query.minResults.isDefined) query.minResults else defaultMinRes
        val maxRes = if (query.maxResults.isDefined) query.maxResults else defaultMaxRes

        if ( ( for ( mnR <- minRes; mxR <- maxRes ) yield mxR - mnR ).exists( _ < 0 ) )
          throw new BadQueryParameterException( "min is greater than max" )
        else {
            cdr.annotations match {
                case null => List.empty
                case allAnnotations : List[ CdrAnnotation[ _ ] ] =>
                    if (query.tagId.isEmpty || query.tagType.isEmpty) throw new QueryValidationException( "tagId/tagType", s"${query.tagId.getOrElse("<empty>")}/${query.tagType.getOrElse("<empty>")}", "tagId and tagType must be defined for document-level aggregation" )
                    if ( !tagIdMap.contains(query.tagId.get) ) throw new QueryValidationException( "tagId", query.tagId.get, s"${query.tagId.get} is not a valid tag identifier" )

                    val tagLabel = tagIdMap( query.tagId.get.toLowerCase )
                    val tagTag = tagTypeMap.getOrElse( query.tagType.get.toLowerCase, query.tagType.get )

                    // Get all values that meet the query criteria
                    val valuesList : List[String] = ( allAnnotations collect { case annotation : OffsetTagAnnotation => annotation } )
                      .withFilter( _.label == tagLabel )
                      .flatMap( _.content.withFilter( _.tag == tagTag ).map( ( tag : OffsetTag ) => {
                          tag.value.get
                      } ) )

                    val allCounts : List[ ValueCount ] = ( valuesList.groupBy( identity ) map {
                        case (value, list) => ValueCount( list.length, value )
                    } )
                      .toList
                      .sortBy( _.count )( Ordering.Int.reverse )

                    val withMin = minRes match {
                        // If no minimum, just get all VCs in a flat list
                        case None => allCounts
                        // If there is a minimum, get all VCs up to that number of elements, but
                        // also include
                        // all other VCs with the same count
                        case Some( min ) =>
                            val minValues = allCounts.take( min )
                            val restValues = allCounts.drop( min )
                            val lastMinCount = minValues.lastOption.map( _.count )
                            lastMinCount match {
                                case None => minValues
                                case Some( lmc ) =>
                                    minValues ++ restValues.takeWhile( _.count == lmc )
                            }

                    }

                    // don't exceed max
                    maxRes match {
                        case None => withMin
                        case Some( max ) => withMin.take( max )
                    }
            }
        }

    }

    def checkStatus : (Boolean, Option[ String ]) = {
        Try( Await.result( Future { docService.count( CorpexSearchRequest() ) }, 8 seconds ) ) match {
            case Success( res ) => (true, None)
            case Failure( e : TimeoutException ) => (false, Some( "Canonical storage timed out" ))
            case Failure( e : Throwable ) => (false, Some( "Canonical storage inaccessible" ))
        }
    }

}

