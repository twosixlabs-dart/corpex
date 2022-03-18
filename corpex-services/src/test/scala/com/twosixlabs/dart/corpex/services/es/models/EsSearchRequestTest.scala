package com.twosixlabs.dart.corpex.services.es.models

import com.twosixlabs.dart.corpex.api.tools.Mapper
import com.twosixlabs.dart.corpex.services.search.es.models.{ BoolQuery, EsQuery, EsSearchRequest, MatchQuery, QueryStringQuery, TermQuery }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class EsSearchRequestTest extends AnyFlatSpecLike with Matchers {

    "EsSearchRequest" should "marshal a full text query successfully" in {
        val queryJson = """{"query":{"query_string":{"fields":["extracted_text"],"query":"oromia"}},"_source":["extracted_metadata.Pages"],"size":1}""".stripMargin

        val queryString = QueryStringQuery( fields = List( "extracted_text" ), query = "oromia" )
        val esQuery = EsQuery( queryStringQuery = Some( queryString ) )
        val req = EsSearchRequest( query = esQuery, size = Some( 1 ), source = Some( List( "extracted_metadata.Pages" ) ) )

        val jsonOutput = Mapper.marshal( req )
        jsonOutput shouldBe queryJson
    }

    "EsSearchRequest" should "marshal a match query successfully" in {
        val queryJson = """{"query":{"match":{"extracted_metadata.Title":{"query":"ethiopia"}}},"_source":["extracted_metadata.Title"],"size":1}"""

        val matchQuery = MatchQuery( "ethiopia" )
        val esQuery = EsQuery( matchQuery = Some( Map( "extracted_metadata.Title" -> matchQuery ) ) )
        val req = EsSearchRequest( query = esQuery, size = Some( 1 ), source = Some( List( "extracted_metadata.Title" ) ) )

        val jsonOutput = Mapper.marshal( req )
        jsonOutput shouldBe queryJson
    }

    "EsSearchRequest" should "marshal a term query successfully" in {
        val queryJson = """{"query":{"term":{"content_type":{"value":"application/json"}}},"_source":["content_type "],"size":1}"""

        val matchQuery = TermQuery( "application/json" )
        val esQuery = EsQuery( termQuery = Some( Map( "content_type" -> matchQuery ) ) )
        val req = EsSearchRequest( query = esQuery, size = Some( 1 ), source = Some( List( "content_type " ) ) )

        val jsonOutput = Mapper.marshal( req )
        jsonOutput shouldBe queryJson
    }

    "EsSearchRequest" should "marshal a bool query successfully" in {
        val queryJson =
            """{"query":{"bool":{"must":[{"match":{"extracted_metadata.Title":{"query":"ethiopia"}}},{"term":{"content_type":{"value":"application/json"}}}],"filter":[{"query_string":{"fields":["extracted_text"],"query":"ethiopia +oromia"}},{"bool":{"should":[{"match":{"extracted_metadata.Publisher":{"query":"New York Times"}}}]}}]}}}""".stripMargin

        val mustQuery1 : EsQuery = EsQuery( matchQuery = Some( Map( "extracted_metadata.Title" -> MatchQuery( "ethiopia" ) ) ) )
        val mustQuery2 : EsQuery = EsQuery( termQuery = Some( Map( "content_type" -> TermQuery( "application/json" ) ) ) )
        val filterQuery1 : EsQuery = EsQuery( queryStringQuery = Some( QueryStringQuery( List( "extracted_text" ), "ethiopia +oromia" ) ) )
        val subShouldQuery = EsQuery( matchQuery = Some( Map( "extracted_metadata.Publisher" -> MatchQuery( "New York Times" ) ) ) )
        val filterQuery2 : EsQuery = EsQuery( boolQuery = Some( BoolQuery( should = Some( List( subShouldQuery ) ) ) ) )
        val boolQuery = EsQuery( boolQuery = Some( BoolQuery( must = Some( List( mustQuery1, mustQuery2 ) ), filter = Some( List( filterQuery1, filterQuery2 ) ) ) ) )

        val req = EsSearchRequest( query = boolQuery )

        val jsonOutput = Mapper.marshal( req )
        jsonOutput shouldBe queryJson
    }

}
