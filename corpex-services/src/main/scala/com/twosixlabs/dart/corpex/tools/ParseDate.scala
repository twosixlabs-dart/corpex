package com.twosixlabs.dart.corpex.tools

import com.twosixlabs.dart.corpex.api.models.MultiValue
import com.twosixlabs.dart.corpex.api.models.queries.CorpexFieldAggQuery
import com.twosixlabs.dart.utils.DatesAndTimes

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

object ParseDate {

    // ES returns date-time strings without a colon in the zone offset; this is valid according to iso 8601
    // but OffsetDateTime won't/can't parse it
    val isoDateParse : Regex = """(.+\+)(\d\d)(\d\d)$""".r

    def getDateFromStr( dateStr : String ) : OffsetDateTime = {
        dateStr match {
            case isoDateParse( pref : String, hours : String, mins : String ) =>
                DatesAndTimes.fromIsoOffsetDateTimeStr( s"${pref}${hours}:${mins}" )
            case _ =>
                Try ( DatesAndTimes.fromIsoOffsetDateTimeStr( dateStr ) ) match {
                    case Success( res ) => res
                    case Failure( _ ) =>
                        Try( DatesAndTimes.fromIsoLocalDateStr( dateStr ) ) match {
                            case Success( res ) => res.atStartOfDay( ZoneOffset.UTC ).toOffsetDateTime
                            case Failure( e : Throwable ) => throw e
                        }
                }
        }
    }

    def getMsIntervalFromChar( str: String ) : Long = str match {
        case "ms" => 1L
        case "s" => 1000L
        case "m" => 60000L
        case "h" => 3600000L
        case "d" => 86400000L
        case "w" => 604800000L
        case "M" => 2628000000L
        case "q" => 7884000000L
        case "y" => 31536000000L
    }

    def getMsIntervalFromString( intervalString : String ) : Long = {
        val intPattern = "([0-9]+)([mshdwMqy]+)".r
        val charPattern = "([mshdwMqy]+)".r

        intervalString match {
            case intPattern( intFactor : String, intStr : String ) =>
                intFactor.toLong * getMsIntervalFromChar( intStr )
            case charPattern( intStr : String ) =>
                getMsIntervalFromChar( intStr )
        }
    }

    def addMsToDateStr( dateStr : String, interval : Long ) : String = {
        val date = getDateFromStr( dateStr )
        val dateMs : Long = date.toInstant.toEpochMilli
        val dateHiMs = dateMs + interval
        val dateHi = OffsetDateTime.ofInstant( Instant.ofEpochMilli( dateHiMs ), ZoneOffset.UTC )
        DatesAndTimes.toIsoOffsetDateTimeStr( dateHi )
    }

    def getMsIntervalFromCorpexAggQuery( corpexAggQuery : CorpexFieldAggQuery ) : Long = corpexAggQuery.bucketSize match {
        case MultiValue( None, None, None ) => ParseQuery.getDataIdFromAggQuery( corpexAggQuery ).defaultBucketSize match {
            case None => 31536000000L
            case Some( MultiValue( Some( es : Long ), None, None ) ) => es
            case Some( MultiValue( None, Some( str ), None ) ) => getMsIntervalFromString( str )
        }
        case MultiValue( Some( long ), None, None) => long
        case MultiValue( None, Some( string ), None ) => getMsIntervalFromString( string )
    }

    def getHiDateStr( dateStr : String, corpexAggQuery : CorpexFieldAggQuery ) : String = {
        val interval = getMsIntervalFromCorpexAggQuery( corpexAggQuery )
        addMsToDateStr( dateStr, interval )
    }

    def getHiDateMs( dateMs : Long, corpexAggQuery : CorpexFieldAggQuery ) : Long = {
        dateMs + getMsIntervalFromCorpexAggQuery( corpexAggQuery )
    }

}
