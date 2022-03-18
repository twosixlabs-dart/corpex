package com.twosixlabs.dart.corpex.services.aggregation.helpers

import com.twosixlabs.dart.corpex.services.aggregation.models.AggregationQuery
import com.twosixlabs.dart.exceptions.BadRequestBodyException
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.util.{ Failure, Success, Try }

class HelpersTest extends AnyFlatSpecLike with Matchers with MockFactory {

    behavior of "unmarshalQueryRequest"

    it should "successfully create an AggregationQuery case from a valid query definition json string" in {
        val queryJson = """{"tag_id":"test-tag-id","tag_type":"test-tag-type","min_results":10}"""

        val query : AggregationQuery = unmarshalQueryRequest( queryJson )

        query.tagId.get shouldBe "test-tag-id"
        query.tagType.get shouldBe "test-tag-type"
        query.facetId shouldBe None
        query.fieldId shouldBe None
        query.minResults.get shouldBe 10
        query.maxResults shouldBe None
    }

    it should "throw a bad request body exception from an invalid json string" in {
        val queryJson = """{"tag_id",:some id}"""

        Try( unmarshalQueryRequest( queryJson ) ) match {
            case Failure( e : BadRequestBodyException ) => e.getMessage should include ( "Unable to parse request body" )
            case Failure( e : Throwable ) => fail( s"${e.getClass}, ${e.getMessage}" )
            case Success( query ) => fail( query.toString )
        }
    }

    it should "throw a bad request body exception from a valid json string with invalid parameter combinations" in {
        val queryJson = """{"tag_id":"test-tag-id","min_results":10}"""

        Try( unmarshalQueryRequest( queryJson ) ) match {
            case Failure( e : BadRequestBodyException ) => e.getMessage should include ( "tagId" )
            case Failure( e : Throwable ) => fail( s"${e.getClass}, ${e.getMessage}" )
            case Success( query ) => fail( query.toString )
        }
    }

}
