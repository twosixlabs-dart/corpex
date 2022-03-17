package com.twosixlabs.dart.corpex.services.es.models

import better.files.Resource
import com.twosixlabs.cdr4s.json.dart.DartCdrDocumentDto
import com.twosixlabs.dart.corpex.api.tools.Mapper
import org.scalatest.{FlatSpecLike, Matchers}

class EsDocumentResponseTest extends FlatSpecLike with Matchers {
    "Mapper" should "unmarshal a cdr" in {
        val cdrJson = Resource.getAsString( "test_cdr.json" )
        val cdrObj = Mapper.unmarshal( cdrJson, classOf[ DartCdrDocumentDto ] )

        cdrObj.documentId shouldBe "78f66304711008e1c38a96af2481a208"
    }

    "EsSearchResponse" should "unmarshal a single document request" in {
        val testEsResponse = Resource.getAsString( "test_es_cdr.json" )
        val esResponse = Mapper.unmarshal( testEsResponse, classOf[ EsDocumentResponse ] )

        esResponse.dartDoc.cdr should not be null
        esResponse.dartDoc.cdr.documentId shouldBe "78f66304711008e1c38a96af2481a208"
    }
}
