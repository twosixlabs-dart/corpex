package com.twosixlabs.dart.corpex.services.es

import com.twosixlabs.dart.commons.config.StandardCliConfig
import com.twosixlabs.dart.test.tags.annotations.IntegrationTest
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

@IntegrationTest
class EsSearchTest extends AnyFlatSpecLike with Matchers {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    // This test should only be run with an es instance running on 9200 loaded with
    // jan 2020 experiment cdrs (ethiopia/oromia, food security scenario)

    val inGitlab = System.getenv( "GITLAB_CI" ) != null
    val env = if (inGitlab) "gitlab_integration_test" else "integration_test"

    val config = ConfigFactory.load( "test" )

    val esService = ElasticsearchSearchService( config )

    // TODO: get elasticsearch integration working in gitlab

//    "ElasticsearchService.search()" should "return results from a full text search" in {
//        val corpexTextQuery = CorpexTextQuery( boolType = BoolType.MUST,
//                                               queriedFields = List( "cdr.extracted_text" ),
//                                               queryString = "russia" )
//
//        val corpexDateQuery = CorpexCdrDateQuery( boolType = BoolType.MUST,
//                                                  queriedFields = List( "cdr.extracted_metadata.CreationDate" ),
//                                                  dateLo = MultiValue( long = Some( 1452254159000L ) ),
//                                                  dateHi = MultiValue( long = Some( 1490792399000L ) ) )
//
//        val corpexRequest = CorpexSearchRequest( queries = Some( List( corpexTextQuery, corpexDateQuery ) ) )
//
//        val results : CorpexSearchResults = esService.search( corpexRequest )
//
//        results.numResults shouldBe 152
//        results.exactNum shouldBe true
//        results.numPages shouldBe None
//        results.pageSize shouldBe None
//        results.results.isDefined shouldBe true
//        val allRes = results.results.get
//        allRes.length shouldBe 100
//        allRes.foreach( res => {
//            res.cdr.documentId shouldNot be( null )
//        } )
//        allRes.count( ( res : CorpexSingleResult ) => res.cdr.extractedMetadata == null ) < 10 shouldBe true
//    }

//    "ElasticsearchService.searchGetAll()" should "return a map with all doc-ids in it" in {
//        val corpexTextQuery = CorpexSearchQuery( boolType = BoolType.MUST,
//                                                 queryType = QueryType.TEXT,
//                                                 queryString = Some( "*" ) )
//
//        val corpexRequest = CorpexSearchRequest( query = List( corpexTextQuery ) )
//
//        val results = esService.searchGetAll( corpexRequest )
//
//        results.size shouldBe 44049
//
//    }
}
