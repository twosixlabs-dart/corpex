package com.twosixlabs.dart.corpex.api.tools

import java.time.{Instant, OffsetDateTime, ZoneOffset}

import com.twosixlabs.dart.corpex.api.exceptions.InvalidSearchQueryException
import com.twosixlabs.dart.corpex.api.models.MultiValue
import com.twosixlabs.dart.utils.DatesAndTimes

object DateUtil {
    private val EsDatePattern = """(.+\+)(\d\d)(\d\d)$""".r
    private val DateTimePattern = "^([0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-6][0-9]:[0-6][0-9].\\d+Z)$".r
    private val DatePattern = "^([0-9]{4})-([01][0-9])-([0-3][0-9])$".r
    private val MonthPattern = "^([0-9]{4})-([01][0-9])$".r
    private val YearPattern = "^([0-9]{4})$".r


    def getDateFromMultiValue( mvDate: MultiValue, lo : Boolean ) : OffsetDateTime = mvDate.get() match {
        case ms : Long => OffsetDateTime.ofInstant( Instant.ofEpochMilli( ms ), ZoneOffset.UTC )
        case str : String => str match {
            case EsDatePattern( pref : String, hours : String, mins : String ) =>
                DatesAndTimes.fromIsoOffsetDateTimeStr( s"${pref}${hours}:${mins}" )

            case DateTimePattern( date ) =>
                DatesAndTimes.fromIsoOffsetDateTimeStr( date )

            case DatePattern( year, month, day ) =>
                OffsetDateTime.of( year.toInt, month.toInt, day.toInt, if ( lo ) 0 else 23, if ( lo ) 0 else 59, if ( lo ) 0 else 59, if ( lo ) 0 else 999999999, ZoneOffset.UTC )

            case MonthPattern( year, month ) =>
                val date = OffsetDateTime.of( year.toInt, month.toInt, 1, if ( lo ) 0 else 23, if ( lo ) 0 else 59, if ( lo ) 0 else 59, if ( lo ) 0 else 999999999, ZoneOffset.UTC )
                if ( lo ) date else date.withDayOfMonth( date.getMonth.length( date.toLocalDate.isLeapYear ) )

            case YearPattern( year ) =>
                OffsetDateTime.of( year.toInt, if ( lo ) 1 else 12, if ( lo ) 1 else 31, if ( lo ) 0 else 23, if ( lo ) 0 else 59, if ( lo ) 0 else 59, if ( lo ) 0 else 999999999, ZoneOffset.UTC )

            case _ => DatesAndTimes.fromIsoOffsetDateTimeStr( str )
        }

        case _ => throw new InvalidSearchQueryException( s"date fields do not support floating point values" )
    }
}
