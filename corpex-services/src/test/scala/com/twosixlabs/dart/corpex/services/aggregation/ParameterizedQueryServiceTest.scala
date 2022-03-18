package com.twosixlabs.dart.corpex.services.aggregation

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class ParameterizedQueryServiceTest extends AnyFlatSpecLike with Matchers with MockFactory {

    val queryService = new ParameterizedQueryService(
        Map( "predefined.query.test-query-1" -> "tagId:tag-id-1,tagType:tag-type-1",
             "predefined.query.test-query-2" -> "tagId:tag-id-1,tagType:tag-type-2",
             "predefined.query.test-query-3" -> "tagId:tag-id-2,tagType:tag-type-a,minResults:10" ) )

    behavior of "QueryService.parseParams"

    it should "return a query from parameters if queryName is empty" in {
        val query = queryService.parseParams( None, Some( "test-tag-id" ), Some( "test-tag-type" ), None, None, minResults = Some( "100" ), None )
        query.tagId shouldBe Some( "test-tag-id" )
        query.tagType shouldBe Some( "test-tag-type" )
        query.minResults shouldBe Some( 100 )
        query.facetId shouldBe None
        query.fieldId shouldBe None
        query.maxResults shouldBe None
    }

    it should "return a predefined query if no other params are set" in {
        val query = queryService.parseParams( Some( "test-query-1" ), None, None, None, None, None, None )
        query.tagId shouldBe Some( "tag-id-1" )
        query.tagType shouldBe Some( "tag-type-1" )
        query.fieldId shouldBe None
        query.facetId shouldBe None
        query.minResults shouldBe None
        query.maxResults shouldBe None
    }

    it should "return a predefined query with parameters updated by new parameters" in {
        val query = queryService.parseParams( Some( "test-query-3" ), None, Some( "tag-type-b" ), None, None, None, Some( "150" ) )
        query.tagId shouldBe Some( "tag-id-2" )
        query.tagType shouldBe Some( "tag-type-b" )
        query.fieldId shouldBe None
        query.facetId shouldBe None
        query.minResults shouldBe Some( 10 )
        query.maxResults shouldBe Some( 150 )
    }

    behavior of "ParameterizedQueryService.getQueryMapFromProperties"

    it should "parse a collection of valid properties into a map of query names to AggregationQuery objects" in {

        val validProps = Map( "predefined.query.location" -> "tagId:qntfy-ner,tagType:LOC,minResults:10",
                              "predefined.query.action" -> "tagId: qntfy-event, tagType :B-action ,maxResults:30",
                              "predefined.query.publisher" -> "fieldId:cdr.extracted_metadata.Publisher" )

        val queryMap = queryService.getQueryMapFromProperties( validProps )

        queryMap.size shouldBe 3
        queryMap( "location" ).facetId shouldBe None
        queryMap( "location" ).fieldId shouldBe None
        queryMap( "location" ).maxResults shouldBe None
        queryMap( "location" ).tagId shouldBe Some( "qntfy-ner" )
        queryMap( "location" ).tagType shouldBe Some( "LOC" )
        queryMap( "location" ).minResults shouldBe Some( 10 )
        queryMap( "action" ).facetId shouldBe None
        queryMap( "action" ).fieldId shouldBe None
        queryMap( "action" ).maxResults shouldBe Some( 30 )
        queryMap( "action" ).tagId shouldBe Some( "qntfy-event" )
        queryMap( "action" ).tagType shouldBe Some( "B-action" )
        queryMap( "action" ).minResults shouldBe None
        queryMap( "publisher" ).facetId shouldBe None
        queryMap( "publisher" ).fieldId shouldBe Some( "cdr.extracted_metadata.Publisher" )
        queryMap( "publisher" ).maxResults shouldBe None
        queryMap( "publisher" ).tagId shouldBe None
        queryMap( "publisher" ).tagType shouldBe None
        queryMap( "publisher" ).minResults shouldBe None

    }

}
