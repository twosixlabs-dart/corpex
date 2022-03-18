package com.twosixlabs.dart.corpex.services.aggregation.models

import com.twosixlabs.dart.exceptions.BadQueryParameterException
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.util.{ Failure, Try }

class AggregationQueryTest extends AnyFlatSpecLike with Matchers with MockFactory {

    behavior of "AggregationQuery.fromParams"

    it should "generate an AggregationQuery case when parameters are valid" in {
        val query = AggregationQuery.fromParams( Some( "test-tag-id" ), Some( "test-tag-type" ), maxResults = Some( "50" ) )
        query.maxResults.get shouldBe 50
        query.tagId.get shouldBe "test-tag-id"
        query.tagType.get shouldBe "test-tag-type"
        query.fieldId shouldBe None
        query.facetId shouldBe None
        query.minResults shouldBe None

        val query2 = AggregationQuery.fromParams( fieldId = Some( "test-field-id" ), minResults = Some( "5" ) )
        query2.minResults.get shouldBe 5
        query2.tagId shouldBe None
        query2.tagType shouldBe None
        query2.facetId shouldBe None
        query2.fieldId.get shouldBe "test-field-id"
        query2.maxResults shouldBe None
    }

    it should "throw bad parameter exceptions when invalid parameters are provided" in {
        Try( AggregationQuery.fromParams( Some( "value" ) ) ) match {
            case Failure( e : BadQueryParameterException ) => e.getMessage should include ( "tagId" )
            case Failure( e : Throwable ) => fail( s"${e.getClass}: ${e.getMessage}" )
            case _ => fail
        }

        Try( AggregationQuery.fromParams( Some( "value" ), Some( "value" ), Some( "value" ) ) ) match {
            case Failure( e : BadQueryParameterException ) =>
                e.getMessage should include ( "tagId" )
                e.getMessage should include ( "facetId" )
            case _ => fail
        }

        Try( AggregationQuery.fromParams( Some( "value" ), Some( "value" ), Some( "value" ) ) ) match {
            case Failure( e : BadQueryParameterException ) =>
                e.getMessage should include ( "tagId" )
                e.getMessage should include ( "facetId" )
            case _ => fail
        }

        Try( AggregationQuery.fromParams( facetId = Some( "value" ), fieldId = Some( "value" ) ) ) match {
            case Failure( e : BadQueryParameterException ) =>
                e.getMessage should include ( "fieldId" )
                e.getMessage should include ( "facetId" )
            case _ => fail
        }

        Try( AggregationQuery.fromParams( facetId = Some( "value" ), minResults = Some( "bad-value" ) ) ) match {
            case Failure( e : BadQueryParameterException ) =>
                e.getMessage should include ( "minResults" )
                e.getMessage should include ( "Integer" )
            case _ => fail
        }

        Try( AggregationQuery.fromParams( facetId = Some( "value" ), maxResults = Some( "0" ) ) ) match {
            case Failure( e : BadQueryParameterException ) =>
                e.getMessage should include ( "maxResults" )
                e.getMessage should include ( "more than 0" )
            case _ => fail
        }

        Try( AggregationQuery.fromParams( facetId = Some( "value" ), minResults = Some( "30" ), maxResults = Some( "10" ) ) ) match {
            case Failure( e : BadQueryParameterException ) =>
                e.getMessage should include ( "maxResults" )
                e.getMessage should include ( "greater than or equal to minResults" )
            case _ => fail
        }

    }



}
