package com.twosixlabs.dart.corpex.controller

import better.files.Resource
import com.twosixlabs.cdr4s.json.dart.{ DartCdrDocumentDto, DartJsonFormat }
import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.groups.ProgramManager
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.commons.config.StandardCliConfig
import com.twosixlabs.dart.corpex.api.models.ValueCount
import com.twosixlabs.dart.corpex.services.aggregation.{ CdrService, ParameterizedQueryService }
import com.twosixlabs.dart.corpex.services.search.SearchService
import com.twosixlabs.dart.exceptions.ResourceNotFoundException
import com.twosixlabs.dart.json.JsonFormat
import com.twosixlabs.dart.test.tags.WipTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatra.test.scalatest.ScalatraSuite
import org.slf4j.{ Logger, LoggerFactory }

import javax.servlet.http.HttpServletRequest
import scala.collection.JavaConverters._
import scala.util.{ Failure, Success }

class CdrAggregationControllerTest extends AnyFlatSpecLike with ScalatraSuite with Matchers with MockFactory with StandardCliConfig {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    val props : Map[String, String ] = processConfig( Array( "--env", "test" ) ).asScala.toMap

    val queryService = new ParameterizedQueryService(
        Map( "predefined.query.test-query-1" -> "tagId:qntfy-ner,tagType:DATE,maxResults:3",
             "predefined.query.test-query-2" -> "tagId:qntfy-event,tagType:NIL",
             "predefined.query.test-query-3" -> "tagId:qntfy-ner,tagType:LOC,minResults:10" ) )

    val docService = stub[ SearchService ]

    val cdrService = new CdrService( props, docService )

    val baseDeps = CorpexBaseController.FromAuthDeps( SecureDartController.deps( "agg", None, false, Nil ) )

    addServlet( new CdrAggregationController( queryService, cdrService, baseDeps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = DartUser( "super-user", Set( ProgramManager ) )
    }, "/*" )

    val testCdrJson = Resource.getAsString( "test_cdr.json" )
    val testCdrDto = JsonFormat.unmarshalTo( testCdrJson, classOf[ DartCdrDocumentDto ] ).get
    val cdr1 = (new DartJsonFormat).unmarshalCdr( testCdrJson ).get

    behavior of "GET /documents/:docId/aggregate"

    it should "return 200 and a json list of value-count objects if docId exists and query parameters are valid" in {
        ( docService.getDocument _ )
          .when( "test-doc-id", *, * )
          .returns( testCdrDto )

        get( "/test-doc-id/aggregate?tagId=qntfy-ner&tagType=DATE&maxResults=3" ) {
            status shouldBe 200
            response.body shouldBe """[{"count":84,"value":"2020"},{"count":69,"value":"2019"},{"count":45,"value":"2018"}]"""
        }
    }

    it should "return 200 and a json list of value-count objects if docId exists and query parameters are valid and a valid date query parameter is provided" in {
        ( docService.getDocument _ )
          .when( "test-doc-id", *, * )
          .returns( testCdrDto )

        get( "/test-doc-id/aggregate?tagId=qntfy-ner&tagType=DATE&maxResults=3&date=1985-06-08" ) {
            status shouldBe 200
            response.body shouldBe """[{"count":84,"value":"2020"},{"count":69,"value":"2019"},{"count":45,"value":"2018"}]"""
        }
    }

    it should "return 200 and a full json list of value-count objects if docId exists, query parameters are valid, and no min or max is set" in {
        ( docService.getDocument _ )
          .when( "test-doc-id", *, * )
          .returns( testCdrDto )

        get( "/test-doc-id/aggregate?tagId=qntfy-ner&tagType=DATE" ) {
            status shouldBe 200
            JsonFormat.unmarshalTo( response.body, classOf[ List[ ValueCount ] ] ).get.length shouldBe 402
        }
    }

    it should "return 404 and a failure json object if cdrRepository returns resource not found exception" in {
        ( docService.getDocument _ )
          .when( "test-doc-id", *, * )
          .throws( new ResourceNotFoundException( "CDR", Some( "test-doc-id" ) ) )

        get( "/test-doc-id/aggregate?tagId=qntfy-ner&tagType=DATE&maxResults=3" ) {
            status shouldBe 404
            response.body should include ( "404" )
            response.body should include ( "test-doc-id" )
        }
    }

    it should "return 503 and a failure json object if cdrRepository returns a Failure of any kind" in {
        ( docService.getDocument _ )
          .when( "test-doc-id", *, * )
          .throws( new IndexOutOfBoundsException( "test error" ) )

        get( "/test-doc-id/aggregate?tagId=qntfy-ner&tagType=DATE&maxResults=3" ) {
            status shouldBe 503
            response.body should include ( "503" )
            response.body should include ( "document service" )
        }
    }

    it should "return 400 and a failure json object if either tagId or tagType is undefined" in {
        get( "/test-doc-id/aggregate?tagType=DATE&maxResults=3" ) {
            status shouldBe 400
            response.body should include ( "400" )
            response.body should include ( "Bad request" )
            response.body should include ( "tagId=<empty>" )
        }
    }

    it should "return 400 and a failure json object if tagId and tagType are defined, but so is fieldId or facetId" in {
        ( docService.getDocument _ )
          .when( "test-doc-id", *, * )
          .returns( testCdrDto )

        get( "/test-doc-id/aggregate?tagId=qntfy-ner&tagType=DATE&maxResults=3&facetId=qntfy-topic" ) {
            status shouldBe 400
            response.body should include ( "400" )
            response.body should include ( "Bad request" )
            response.body should include ( "facetId" )
        }
    }

    it should "return 200 and a json list of value-count objects if docId exists and valid queryName is provided with no other parameters" in {
        ( docService.getDocument _ )
          .when( "test-doc-id", *, * )
          .returns( testCdrDto )

        get( "/test-doc-id/aggregate?queryName=test-query-1" ) {
            status shouldBe 200
            response.body shouldBe """[{"count":84,"value":"2020"},{"count":69,"value":"2019"},{"count":45,"value":"2018"}]"""
        }
    }

    it should "return 200 and a different json list of value-count objects if docId exists and valid queryName is provided with other parameters to update it" in {
        ( docService.getDocument _ )
          .when( "test-doc-id", *, * )
          .returns( testCdrDto )

        get( "/test-doc-id/aggregate?queryName=test-query-1&minResults=100&maxResults=200" ) {
            status shouldBe 200
            response.body should startWith ( """[{"count":84,"value":"2020"},{"count":69,"value":"2019"},{"count":45,"value":"2018"},{"count":42,"value":"2017"},{"count":39,"value":"2015"}""" )
            val valCounts = JsonFormat.unmarshalTo( response.body, classOf[ List[ ValueCount ] ] ).get // NOTE: doesn't correctly unmarshal for some reason...
            valCounts.length should be > 10
        }
    }

    it should "return 400 and a json failure response object if docId exists but invalid queryName is provided" in {
        ( docService.getDocument _ )
          .when( "test-doc-id", *, * )
          .returns( testCdrDto )

        get( "/test-doc-id/aggregate?queryName=bad-query-name" ) {
            status shouldBe 400
            response.body should include ( "queryName=bad-query-name" )
        }
    }

    behavior of "GET /documents/:docId/aggregate-all"

    it should "return 200 and a json object mapping query names to value count lists if docId is correct with no query parameters" in {
        ( docService.getDocument _ )
          .when( "test-doc-id", *, * )
          .returns( testCdrDto )

        get( "/test-doc-id/aggregate-all" ) {
            status shouldBe 200
            response.body should startWith ( """{"test-query-1":[{"count":""" )
            response.body should include ( """"test-query-2":[{""" )
            response.body should include ( """"test-query-3":[{""" )
        }
    }

}
