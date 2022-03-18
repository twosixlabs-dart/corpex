package com.twosixlabs.dart.corpex.services.aggregation

import better.files.Resource
import com.twosixlabs.cdr4s.json.dart.DartJsonFormat
import com.twosixlabs.dart.commons.config.StandardCliConfig
import com.twosixlabs.dart.corpex.services.aggregation.models.AggregationQuery
import com.twosixlabs.dart.corpex.services.search.SearchService
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.slf4j.{ Logger, LoggerFactory }

import scala.collection.JavaConverters._

class CdrServiceTest extends AnyFlatSpecLike with Matchers with MockFactory with StandardCliConfig {

    val LOG : Logger = LoggerFactory.getLogger( getClass )

    val props : Map[String, String ] = processConfig( Array( "--env", "test" ) ).asScala.toMap

    val docService = stub[ SearchService ]

    val cdrFormat = new DartJsonFormat

    val cdr1 = cdrFormat.unmarshalCdr( Resource.getAsString( "test_cdr.json" ) ).get

    val query1 = AggregationQuery( Some( "qntfy-ner" ), Some( "DATE" ) )

    behavior of "CdrService"

    it should "correctly parse annotation label map from properties parameter" in {
        val cdrService = new CdrService( props, docService )
        cdrService.tagIdMap( "qntfy-ner" ) shouldBe "qntfy-ner-annotator"
    }

    behavior of "CdrService.aggregateCdr"

    it should "return a list of value count objects in ordered by count (descending) when passed a cdr with tag annotations and a valid AggregationQuery case" in {
        val cdrService = new CdrService( props, docService )
        val valueCounts = cdrService.aggregateCdr( cdr1, query1 )

        valueCounts.zip( valueCounts.tail ).foreach( tup => {
            val (left, right) = tup
            left.count should be >= right.count
        } )
    }

    it should "return a list of value count objects of length equal to minResults if there is only one ValueCount with that count" in {

        val cdrService = new CdrService( props, docService )
        val valueCounts = cdrService.aggregateCdr( cdr1, query1.copy( minResults = Some( 3 ) ) )

        valueCounts.length shouldBe 3
        valueCounts.zip( valueCounts.tail ).foreach( tup => {
            val (left, right) = tup
            left.count should be >= right.count
        } )
    }

    it should "return a list of value count objects of length equal to maxResults if minResults is not set" in {
        val cdrService = new CdrService( props, docService )
        val valueCounts = cdrService.aggregateCdr( cdr1, query1.copy( minResults = None, maxResults = Some( 10 ) ) )

        valueCounts.length shouldBe 10
        valueCounts.zip( valueCounts.tail ).foreach( tup => {
            val (left, right) = tup
            left.count should be >= right.count
        } )
    }

    it should "return all results if neither maxResults nor minResults are set" in {
        val cdrService = new CdrService( props, docService )
        val valueCounts = cdrService.aggregateCdr( cdr1, query1.copy( minResults = None, maxResults = None ) )

        valueCounts.length shouldBe 402
        valueCounts.zip( valueCounts.tail ).foreach( tup => {
            val (left, right) = tup
            left.count should be >= right.count
        } )
    }

    it should "return a list of value counts longer than min but shorter than max if there are more values of the same count as the min result between min and max" in {
        val cdrService = new CdrService( props, docService )
        val valueCounts = cdrService.aggregateCdr( cdr1, AggregationQuery( Some( "qntfy-event" ), Some( "NIL" ), minResults = Some( 15 ), maxResults = Some( 20 ) ) )

        valueCounts.length shouldBe 17
        valueCounts.zip( valueCounts.tail ).foreach( tup => {
            val (left, right) = tup
            left.count should be >= right.count
        } )
    }

    it should "return an empty list when there are no tags of a queried type" in {
        val cdrService = new CdrService( props, docService )
        val valueCounts = cdrService.aggregateCdr( cdr1, AggregationQuery( Some( "qntfy-event" ), Some( "non-existent-tag" ), minResults = Some( 4 ), maxResults = Some( 20 ) ) )

        valueCounts shouldBe List.empty
    }

}
