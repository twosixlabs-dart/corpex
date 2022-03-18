package com.twosixlabs.dart.corpex.api.models

import better.files.Resource
import com.twosixlabs.cdr4s.json.dart.{DartCdrDocumentDto, DartMetadataDto}
import com.twosixlabs.dart.corpex.api.tools.Mapper
import org.scalatest.{FlatSpecLike, Matchers}

class CorpexSearchResultsTest extends FlatSpecLike with Matchers {

    // Timestamp needs to be set as null, since it's a required field, and
    // annotations defaults to empty (when it's set as null, it is marshalled
    // as null for some reason, rather than ignored like other null fields...

    "Mapper.marshal()" should "produce JSON with only the CDR fields we need" in {
        val cdr = DartCdrDocumentDto( documentId = "test_id",
                                      extractedMetadata = DartMetadataDto( title = "Test title" ),
                                      timestamp = null )

        val sr = CorpexSearchResults( numResults = 1,
                                      exactNum = true,
                                      page = Some( 0 ),
                                      numPages = Some( 1 ),
                                      pageSize = Some( 1 ),
                                      results = Some( List( CorpexSingleResult( esScore = Some( 0.678 ), cdr = cdr ) ) ) )

        val srJson = Mapper.marshal( sr )

        srJson shouldBe
        """{"num_results":1,"exact_num":true,"page":0,"num_pages":1,"page_size":1,"results":[{"es_score":0.678,"cdr":{"extracted_metadata":{"Title":"Test title"},"document_id":"test_id","annotations":[]}}]}""".stripMargin

    }

    "Mapper" should "unmarshal a cdr" in {
        val cdrJson = Resource.getAsString( "test_cdr.json" )
        val cdrObj = Mapper.unmarshal( cdrJson, classOf[ DartCdrDocumentDto ] )

        cdrObj.documentId shouldBe "0fc018c8dec5f42c80384244ea87cfce"
    }

}
