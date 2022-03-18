package com.twosixlabs.dart.corpex.services.es.models

import better.files.Resource
import com.twosixlabs.cdr4s.json.dart.DartCdrDocumentDto
import com.twosixlabs.dart.corpex.api.tools.Mapper
import com.twosixlabs.dart.corpex.services.search.es.models.EsDocumentResponse
import org.scalatest.{ FlatSpecLike, Matchers }

class EsDocumentResponseTest extends FlatSpecLike with Matchers {
    "Mapper" should "unmarshal a cdr" in {
        val cdrJson = Resource.getAsString( "test_cdr.json" )
        val cdrObj = Mapper.unmarshal( cdrJson, classOf[ DartCdrDocumentDto ] )

        cdrObj.documentId shouldBe "0fc018c8dec5f42c80384244ea87cfce"
    }

    "EsSearchResponse" should "unmarshal a single document request" in {
        val testEsResponse = Resource.getAsString( "test_es_cdr.json" )
        val esResponse = Mapper.unmarshal( testEsResponse, classOf[ EsDocumentResponse ] )

        esResponse.dartDoc.cdr should not be null
        esResponse.dartDoc.cdr.documentId shouldBe "0fc018c8dec5f42c80384244ea87cfce"
    }
}
