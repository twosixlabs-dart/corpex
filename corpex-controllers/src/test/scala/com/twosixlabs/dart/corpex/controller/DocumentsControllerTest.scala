package com.twosixlabs.dart.corpex.controller

import better.files.Resource
import com.twosixlabs.cdr4s.json.dart.DartCdrDocumentDto
import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.groups.ProgramManager
import com.twosixlabs.dart.auth.tenant.indices.InMemoryCorpusTenantIndex
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.corpex.api.tools.Mapper
import com.twosixlabs.dart.corpex.services.search.SearchService
import com.twosixlabs.dart.utils.DatesAndTimes
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatra.test.scalatest.ScalatraSuite

import javax.servlet.http.HttpServletRequest

class DocumentsControllerTest extends AnyFlatSpecLike with ScalatraSuite with Matchers with MockFactory {

    val mockCdr : String = Resource.getAsString( "test_cdr.json" ).trim
    val mockCdrObj : DartCdrDocumentDto = Mapper.unmarshal( mockCdr, classOf[ DartCdrDocumentDto ] )

    val searchService : SearchService = stub[ SearchService ]

    val config : Config = ConfigFactory.load( "test" ).resolve()

    val baseDependencies : SecureDartController.Dependencies =
        SecureDartController.deps( "corpex", config )

    addServlet( DocumentsController( searchService, new InMemoryCorpusTenantIndex(), baseDependencies ), "/documents/*" )

    "GET /documents/:doc_id" should "return the entire CDR when no parameters are provided" in {
        ( searchService.getDocument _ ).when( "fake_doc_id", *, * ).returns( mockCdrObj )

        get( "/documents/fake_doc_id" ) {
            response.getContentType() shouldBe "application/json;charset=utf-8"
            status shouldBe 200
            response.body shouldBe mockCdr
        }
    }

    "GET /documents/:doc_id" should "return a CDR with only fieldsIncl populated" in {
        ( searchService.getDocument _ )
          .when( "fake_doc_id", Some( "timestamp" ), * )
          .returns( new DartCdrDocumentDto( timestamp = DatesAndTimes.fromIsoOffsetDateTimeStr(
              "2019-12-08T11:55:02.000Z" ), annotations = null ) )

        get( "/documents/fake_doc_id?fieldsIncl=timestamp" ) {
            status shouldBe 200
            response.body shouldBe """{"timestamp":"2019-12-08T11:55:02.000Z","annotations":null}"""
        }
    }

    "GET /documents/:doc_id" should "return a CDR with remaining fields when fieldsExcl is populated" in {
        ( searchService.getDocument _ )
          .when( "fake_doc_id", *, Some( "extracted_text,extracted_metadata,annotations,extracted_ntriples" ) )
          .returns( mockCdrObj.copy( extractedText = null,
                                     extractedMetadata = null,
                                     annotations = null, extractedNtriples = null ) )

        get( "/documents/fake_doc_id?fieldsExcl=extracted_text,extracted_metadata,annotations,extracted_ntriples" ) {
            status shouldBe 200
            response.body shouldBe Resource.getAsString( "test_cdr_excl_fields.json" ).trim
        }
    }

    "GET /documents/:doc_id" should "return a CDR with only fieldsIncl less any overlapping fieldsExcl" in {
        ( searchService.getDocument _ )
          .when( "fake_doc_id", Some( "document_id,extracted_metadata.Publisher,extracted_metadata.Title,extracted_text" ), Some( "extracted_text,timestamp" ) )
          .returns( mockCdrObj.copy( captureSource = null, extractedMetadata = mockCdrObj.extractedMetadata.copy( creationDate = null, modificationDate = null, docType = null,
                                                                                                                  description = null, originalLanguage = null, classification = null,
                                                                                                                  author = null, url = null, pages = null, subject = null,
                                                                                                                  creator = null, producer = null, statedGenre = null ),
                                     contentType = null, extractedNumeric = null, extractedText = null, uri = null, sourceUri = null, extractedNtriples = null, timestamp = null,
                                     annotations = null, labels = null ) )

        get( "/documents/fake_doc_id?fieldsIncl=document_id,extracted_metadata.Publisher,extracted_metadata.Title,extracted_text&fieldsExcl=extracted_text,timestamp" ) {
            status shouldBe 200
            response.body shouldBe
            """{"extracted_metadata":{"Title":"The impact of disasters and crises on agriculture and food security: 2021"},"document_id":"0fc018c8dec5f42c80384244ea87cfce","annotations":null}""".stripMargin
        }
    }

    override def header = null
}
