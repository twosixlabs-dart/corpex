package com.twosixlabs.dart.corpex.controller

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.groups.ProgramManager
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.corpex.services.search.es.ElasticsearchSearchService
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatra.test.scalatest.ScalatraSuite
import org.slf4j.{ Logger, LoggerFactory }

import javax.servlet.http.HttpServletRequest

class DataControllerTest extends AnyFlatSpecLike with ScalatraSuite with Matchers {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    val config : Config = ConfigFactory.load( "test" ).resolve()

    val baseDependencies = SecureDartController.deps( "corpex", config )

    addServlet( new DataController( baseDependencies ), "/*" )

    "GET /fields" should "return a list of profiles for all supported fields" in {
        val expectedJson =
            """[{"field_id":"cdr.capture_source","data_type":"term","cdr_label":"capture_source","label":"Capture Source","description":"Source of ingestion: AutoCollection means obtained through automation or subscription, BackgroundSource means user-uploaded"},{"field_id":"cdr.content_type","data_type":"term","cdr_label":"content_type","label":"Content Type"},{"field_id":"cdr.document_id","data_type":"term","cdr_label":"document_id","label":"Document Id","description":"Canonical identifier of a document, derived from md5 hash of the raw document"},{"field_id":"cdr.extracted_metadata.Author","data_type":"text","cdr_label":"extracted_metadata.Author","label":"Author"},{"field_id":"cdr.extracted_metadata.Classification","data_type":"term","cdr_label":"extracted_metadata.Classification","label":"Classification"},{"field_id":"cdr.extracted_metadata.CreationDate","data_type":"date","cdr_label":"extracted_metadata.CreationDate","label":"Publication Date"},{"field_id":"cdr.extracted_metadata.Creator","data_type":"term","cdr_label":"extracted_metadata.Creator","label":"Creator"},{"field_id":"cdr.extracted_metadata.Description","data_type":"text","cdr_label":"extracted_metadata.Description","label":"Description"},{"field_id":"cdr.extracted_metadata.ModDate","data_type":"date","cdr_label":"extracted_metadata.ModDate","label":"Modification Date"},{"field_id":"cdr.extracted_metadata.OriginalLanguage","data_type":"term","cdr_label":"extracted_metadata.OriginalLanguage","label":"Original Language"},{"field_id":"cdr.extracted_metadata.Pages","data_type":"int","cdr_label":"extracted_metadata.Pages","label":"Pages"},{"field_id":"cdr.extracted_metadata.PredictedGenre","data_type":"term","cdr_label":"extracted_metadata.PredictedGenre","label":"Predicted Genre","description":"Type of document (e.g., academic article, policy paper, news article, etc. Predicted by machine learning analytic"},{"field_id":"cdr.extracted_metadata.Producer","data_type":"term","cdr_label":"extracted_metadata.Producer","label":"Producer"},{"field_id":"cdr.extracted_metadata.Publisher","data_type":"term","cdr_label":"extracted_metadata.Publisher","label":"Publisher"},{"field_id":"cdr.extracted_metadata.StatedGenre","data_type":"term","cdr_label":"extracted_metadata.StatedGenre","label":"User-Defined Genre","description":"Type of document (e.g., academic article, policy paper, news article, etc. Provided by user, not machine learning analytic"},{"field_id":"cdr.extracted_metadata.Subject","data_type":"term","cdr_label":"extracted_metadata.Subject","label":"Subject"},{"field_id":"cdr.extracted_metadata.Title","data_type":"text","cdr_label":"extracted_metadata.Title","label":"Title"},{"field_id":"cdr.extracted_metadata.Type","data_type":"term","cdr_label":"extracted_metadata.Type","label":"Document Type"},{"field_id":"cdr.extracted_text","data_type":"text","cdr_label":"extracted_text"},{"field_id":"cdr.labels","data_type":"term","cdr_label":"labels","label":"Labels","description":"User-defined identifiers (tags)"},{"field_id":"cdr.source_uri","data_type":"term","cdr_label":"source_uri","label":"Source URI","description":"Currently the filename of the raw document as it was submitted to DART"},{"field_id":"cdr.timestamp","data_type":"date","cdr_label":"timestamp","label":"Timestamp","description":"Date and time of ingestion in DART"},{"field_id":"word_count","data_type":"int","cdr_label":"extracted_text.length","label":"Word Count"}]"""

        get( "/" ) {
            status shouldBe 200
            body shouldBe expectedJson
        }
    }

    "GET /fields/:field" should "return a profile for a given field" in {
        get( "/cdr.content_type" ) {
            status shouldBe 200
            body shouldBe """{"field_id":"cdr.content_type","data_type":"term","cdr_label":"content_type","label":"Content Type"}"""
        }
    }

    "GET /fields/:field" should "return 404 for a field that does not exist" in {
        get( "/cdr.bad.field" ) {
            status shouldBe 404
            body should ( include( "status\":404" ) and include( "Resource not found" ) and include( "cdr.bad.field" ) )
        }
    }

    override def header = null
}
