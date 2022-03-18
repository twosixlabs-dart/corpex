package com.twosixlabs.dart.corpex.services.es

import better.files.Resource
import com.twosixlabs.cdr4s.json.dart.DartCdrDocumentDto
import com.twosixlabs.dart.commons.config.StandardCliConfig
import com.twosixlabs.dart.corpex.api.enums.BoolType
import com.twosixlabs.dart.corpex.api.models.CorpexSearchRequest
import com.twosixlabs.dart.corpex.api.models.queries.CorpexTextQuery
import com.twosixlabs.dart.corpex.api.tools.Mapper
import com.twosixlabs.dart.exceptions.BadQueryParameterException
import annotations.WipTest
import com.typesafe.config.ConfigFactory
import okhttp3.mockwebserver.{MockResponse, MockWebServer, RecordedRequest}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.collection.JavaConverters._

class ElasticsearchSearchServiceTest extends AnyFlatSpecLike with StandardCliConfig with Matchers {

    val config = ConfigFactory.load( "test" )

    lazy val esService = ElasticsearchSearchService( config )

    val mockEsJson : String =
        """{
          |  "took": 15,
          |  "timed_out": false,
          |  "_shards": {
          |    "total": 5,
          |    "successful": 5,
          |    "skipped": 0,
          |    "failed": 0
          |  },
          |  "hits": {
          |    "total": 38989,
          |    "max_score": 0.27045804,
          |    "hits": [
          |      {
          |        "_index": "cdr_search",
          |        "_type": "CDR_Document",
          |        "_id": "4ed5bea9d2f4cadfa8a9d215dded5dc3",
          |        "_score": 0.27045804,
          |        "_source": {
          |          "extracted_metadata": {
          |            "Title": "Ambassadors extend New Year wishes, hoping for strengthened bilateral ties"
          |          },
          |          "document_id": "4ed5bea9d2f4cadfa8a9d215dded5dc3"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "CDR_Document",
          |        "_id": "800bb5a15b5c9ced02db3df026ecca3b",
          |        "_score": 0.26871628,
          |        "_source": {
          |          "extracted_metadata": {
          |            "Title": "Ambassador Berhane Receives Copy of the Credentials of the Newly-Appointed Ambassador of Canada"
          |          },
          |          "document_id": "800bb5a15b5c9ced02db3df026ecca3b"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "CDR_Document",
          |        "_id": "993506a5679b229901639e96048f07a6",
          |        "_score": 0.26841372,
          |        "_source": {
          |          "extracted_metadata": {
          |            "Title": "Former US Diplomat Calls for Free, Fair Elections in Ethiopia"
          |          },
          |          "document_id": "993506a5679b229901639e96048f07a6"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "CDR_Document",
          |        "_id": "f6a356221930082a50b56d103f879af3",
          |        "_score": 0.26795644,
          |        "_source": {
          |          "extracted_metadata": {
          |            "Title": "President Mulatu Receives Credentials of Eight Ambassadors"
          |          },
          |          "document_id": "f6a356221930082a50b56d103f879af3"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "CDR_Document",
          |        "_id": "fbfa01bbf857b9e7e3284b781426278f",
          |        "_score": 0.26744416,
          |        "_source": {
          |          "extracted_metadata": {
          |            "Title": "Turkey to invest in Ethiopias hydropower"
          |          },
          |          "document_id": "fbfa01bbf857b9e7e3284b781426278f"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "CDR_Document",
          |        "_id": "6c6486a08ed079ef985fac23f13279e1",
          |        "_score": 0.2673608,
          |        "_source": {
          |          "extracted_metadata": {
          |            "Title": "Swedish H&M Looks Ethiopian Textile Industry"
          |          },
          |          "document_id": "6c6486a08ed079ef985fac23f13279e1"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "CDR_Document",
          |        "_id": "3de48fbc70241db3e57c794f28948ff5",
          |        "_score": 0.2672313,
          |        "_source": {
          |          "extracted_metadata": {
          |            "Title": "Ethiopia Economic Update – Overcoming Inflation, Raising Competitiveness"
          |          },
          |          "document_id": "3de48fbc70241db3e57c794f28948ff5"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "CDR_Document",
          |        "_id": "2c0a670344da82a9130d4e8d98134afe",
          |        "_score": 0.26704112,
          |        "_source": {
          |          "extracted_metadata": {
          |            "Title": "Ethiopia rebukes Human Rights Watch over letter to UK foreign secretary"
          |          },
          |          "document_id": "2c0a670344da82a9130d4e8d98134afe"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "CDR_Document",
          |        "_id": "9468d075a7d8288376ef181f2a44625e",
          |        "_score": 0.2669899,
          |        "_source": {
          |          "extracted_metadata": {
          |            "Title": "China, Ethiopia sign loan agreement"
          |          },
          |          "document_id": "9468d075a7d8288376ef181f2a44625e"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "CDR_Document",
          |        "_id": "0822b6993084e7748405d308015c4ad1",
          |        "_score": 0.2668702,
          |        "_source": {
          |          "extracted_metadata": {
          |            "Title": "Ethiopia: China's successful little brother"
          |          },
          |          "document_id": "0822b6993084e7748405d308015c4ad1"
          |        }
          |      }
          |    ]
          |  }
          |}""".stripMargin

    "ElasticsearchService.search()" should "return an CorpexSearchResults object with mocked results" in {

        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse = new MockResponse()
          .setResponseCode( 200 )
          .setBody( mockEsJson )
        mockServer.enqueue( mockResponse )
        mockServer.start( 8097 )

        val corpexQuery = CorpexTextQuery( BoolType.MUST, queriedFields = List( "cdr.extracted_text" ), queryString = "test query string" )
        val sq = CorpexSearchRequest( queries = Some( List( corpexQuery ) ), pageSize = Some( 10 ), page = Some( 0 ) )
        val sr = esService.search( sq )

        sr.numResults shouldBe 38989
        sr.page.get shouldBe 0
        sr.numPages.get shouldBe 3899
        sr.pageSize.get shouldBe 10
        sr.results.get.length shouldBe 10

        mockServer.shutdown()

    }

    val mockEsCountJson : String =
        """{
          |  "count": 44030,
          |  "_shards": {
          |    "total": 1,
          |    "successful": 1,
          |    "skipped": 0,
          |    "failed": 0
          |  }
          |}
          |""".stripMargin

    "ElasticsearchService.count()" should "return a CorpexSearchResults object" in {

        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse = new MockResponse()
          .setResponseCode( 200 )
          .setBody( mockEsCountJson )
        mockServer.enqueue( mockResponse )
        mockServer.start( 8097 )

        val corpexQuery = CorpexTextQuery( BoolType.MUST, queryString = "test query string", queriedFields = List( "cdr.extracted_text" ) )
        val sq = CorpexSearchRequest( queries = Some( List( corpexQuery ) ) )
        val sr = esService.count( sq )

        sr.numResults shouldBe 44030
        sr.page.isEmpty shouldBe true
        sr.numPages.isEmpty shouldBe true
        sr.pageSize.isEmpty shouldBe true
        sr.results.isEmpty shouldBe true

        mockServer.shutdown()
    }

    "ElasticsearchService.getDocument" should "make a doc request to ES with empty _source_included and _source_excluded parameters when fieldsIncl and fieldsExcl are not set" in {
        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse = new MockResponse()
          .setResponseCode( 200 )
          .setBody( Resource.getAsString( "test_es_cdr.json" ) )
        mockServer.enqueue( mockResponse )
        mockServer.start( 8097 )

        val returnedDoc : DartCdrDocumentDto = esService.getDocument( "fake_doc_id" )
        val cdr = Mapper.unmarshal( Resource.getAsString( "test_cdr.json" ), classOf[ DartCdrDocumentDto ] )

        val request : RecordedRequest = mockServer.takeRequest()
        request.getRequestUrl.toString shouldBe "http://localhost:8097/cdr_search/_doc/fake_doc_id?_source_includes=&_source_excludes="

        mockServer.shutdown()
    }

    "ElasticsearchService.getDocument" should "make a doc request to ES with _source_included and _source_excluded populated from fieldsIncl and fieldsExcl (cleaned up)" in {
        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse = new MockResponse()
          .setResponseCode( 200 )
          .setBody( Resource.getAsString( "test_es_cdr.json" ) )
        mockServer.enqueue( mockResponse )
        mockServer.start( 8097 )

        val returnedDoc : DartCdrDocumentDto = esService.getDocument( "fake_doc_id", Some("document_id, extracted_metadata.Title,annotations "), Some(" annotations ,source_uri") )
        val cdr = Mapper.unmarshal( Resource.getAsString( "test_cdr.json" ), classOf[ DartCdrDocumentDto ] )

        val request : RecordedRequest = mockServer.takeRequest()
        request.getRequestUrl.toString shouldBe "http://localhost:8097/cdr_search/_doc/fake_doc_id?_source_includes=document_id,extracted_metadata.Title,annotations&_source_excludes=annotations,source_uri"

        mockServer.shutdown()
    }

    "ElasticsearchService.getDocument" should "throw a BadRequestParameterException when an invalid CDR field is in either fieldsIncl or fieldsExcl" in {
        val caught = intercept[BadQueryParameterException] {
            esService.getDocument( "fake_doc_id", Some( "document_id, extracted_metadata.Title,fake thing " ) )
        }
        caught.getMessage should include("fieldsIncl=")
        caught.getMessage should include("fake thing")
    }

    "ElasticsearchService.getDocument" should "throw a BadRequestParameterException when an invalid CDR field is in either fieldsExcl but not fieldsIncl" in {
        val caught = intercept[BadQueryParameterException] {
            esService.getDocument( "fake_doc_id", Some( "document_id, extracted_metadata.Title" ), Some( "source_uri, you know hwhat ,annotations" ) )
        }
        caught.getMessage should include("fieldsExcl=")
        caught.getMessage should include("you know hwhat")
    }


    // Below tests are for an implementation of getDocument that does the fields filtering in Corpex. We might
    // need that implementation later, but currently we have removed it since Elasticsearch can do the filtering
    // for us. The above tests verify this new implementation, just checking that the correct requests are sent.

    //    "ElasticsearchService.getDocument" should "return a full DartCdrDocumentDto object when no fields parameters are provided" in {
    //        val mockServer : MockWebServer = new MockWebServer()
    //        val mockResponse = new MockResponse()
    //          .setResponseCode( 200 )
    //          .setBody( Resource.getAsString( "test_es_cdr.json" ) )
    //        mockServer.enqueue( mockResponse )
    //        mockServer.start( 8097 )
    //
    //        val returnedDoc : DartCdrDocumentDto = esService.getDocument( "fake_doc_id" )
    //        val cdr = Mapper.unmarshal( Resource.getAsString( "test_cdr.json" ), classOf[ DartCdrDocumentDto ] )
    //
    //        returnedDoc.documentId shouldBe cdr.documentId
    //        returnedDoc.timestamp shouldBe cdr.timestamp
    //        returnedDoc.contentType shouldBe cdr.contentType
    //        returnedDoc.extractedText shouldBe cdr.extractedText
    //        returnedDoc.sourceUri shouldBe cdr.sourceUri
    //        returnedDoc.captureSource shouldBe cdr.captureSource
    //        returnedDoc.uri shouldBe cdr.uri
    //        returnedDoc.team shouldBe cdr.team
    //        returnedDoc.extractedMetadata.language shouldBe cdr.extractedMetadata.language
    //        returnedDoc.extractedMetadata.classification shouldBe cdr.extractedMetadata.classification
    //        returnedDoc.extractedMetadata.docType shouldBe cdr.extractedMetadata.docType
    //        returnedDoc.extractedMetadata.url shouldBe cdr.extractedMetadata.url
    //        returnedDoc.extractedMetadata.description shouldBe cdr.extractedMetadata.description
    //        returnedDoc.extractedMetadata.creationDate shouldBe cdr.extractedMetadata.creationDate
    //        returnedDoc.extractedMetadata.title shouldBe cdr.extractedMetadata.title
    //        returnedDoc.extractedMetadata.pages shouldBe cdr.extractedMetadata.pages
    //        returnedDoc.extractedMetadata.author shouldBe cdr.extractedMetadata.author
    //        returnedDoc.extractedMetadata.creator shouldBe cdr.extractedMetadata.creator
    //        returnedDoc.extractedMetadata.modificationDate shouldBe cdr.extractedMetadata.modificationDate
    //        returnedDoc.extractedMetadata.producer shouldBe cdr.extractedMetadata.producer
    //        returnedDoc.extractedMetadata.publisher shouldBe cdr.extractedMetadata.publisher
    //        returnedDoc.extractedMetadata.subject shouldBe cdr.extractedMetadata.subject
    //        returnedDoc.extractedMetadata shouldBe cdr.extractedMetadata
    //        returnedDoc.categories shouldBe cdr.categories
    //        returnedDoc.annotations shouldBe cdr.annotations
    //
    //        returnedDoc shouldBe cdr
    //
    //        mockServer.shutdown()
    //    }
    //    "ElasticsearchService.getDocument" should "return just the fields in fieldsIncl, when fieldsExcl is not set" in {
    //        val mockServer : MockWebServer = new MockWebServer()
    //        val mockResponse = new MockResponse()
    //          .setResponseCode( 200 )
    //          .setBody( Resource.getAsString( "test_es_cdr.json" ) )
    //        mockServer.enqueue( mockResponse )
    //        mockServer.start( 8097 )
    //
    //        val returnedDoc : DartCdrDocumentDto = esService.getDocument( "fake_doc_id", Some("document_id,extracted_metadata.Title") )
    //        val cdr =  Mapper.unmarshal( Resource.getAsString( "test_cdr.json" ), classOf[ DartCdrDocumentDto ] )
    //
    //        returnedDoc.documentId shouldBe cdr.documentId
    //        returnedDoc.timestamp shouldBe null
    //        returnedDoc.contentType shouldBe null
    //        returnedDoc.extractedText shouldBe null
    //        returnedDoc.sourceUri shouldBe null
    //        returnedDoc.captureSource shouldBe null
    //        returnedDoc.uri shouldBe null
    //        returnedDoc.team shouldBe null
    //        returnedDoc.extractedMetadata.language shouldBe null
    //        returnedDoc.extractedMetadata.classification shouldBe null
    //        returnedDoc.extractedMetadata.docType shouldBe null
    //        returnedDoc.extractedMetadata.url shouldBe null
    //        returnedDoc.extractedMetadata.description shouldBe null
    //        returnedDoc.extractedMetadata.creationDate shouldBe null
    //        returnedDoc.extractedMetadata.title shouldBe cdr.extractedMetadata.title
    //        returnedDoc.extractedMetadata.pages shouldBe null
    //        returnedDoc.extractedMetadata.author shouldBe null
    //        returnedDoc.extractedMetadata.creator shouldBe null
    //        returnedDoc.extractedMetadata.modificationDate shouldBe null
    //        returnedDoc.extractedMetadata.producer shouldBe null
    //        returnedDoc.extractedMetadata.publisher shouldBe null
    //        returnedDoc.extractedMetadata.subject shouldBe null
    //        returnedDoc.categories shouldBe null
    //        returnedDoc.annotations shouldBe null
    //
    //        mockServer.shutdown()
    //    }
    //
    //    "ElasticsearchService.getDocument" should "return everything but the fields in fieldsExcl, when fieldsIncl is not set" in {
    //        val mockServer : MockWebServer = new MockWebServer()
    //        val mockResponse = new MockResponse()
    //          .setResponseCode( 200 )
    //          .setBody( Resource.getAsString( "test_es_cdr.json" ) )
    //        mockServer.enqueue( mockResponse )
    //        mockServer.start( 8097 )
    //
    //        val returnedDoc : DartCdrDocumentDto = esService.getDocument( "fake_doc_id", None, Some("extracted_metadata,source_uri,categories") )
    //        val cdr =  Mapper.unmarshal( Resource.getAsString( "test_cdr.json" ), classOf[ DartCdrDocumentDto ] )
    //
    //        returnedDoc.documentId shouldBe cdr.documentId
    //        returnedDoc.timestamp shouldBe cdr.timestamp
    //        returnedDoc.contentType shouldBe cdr.contentType
    //        returnedDoc.extractedText shouldBe cdr.extractedText
    //        returnedDoc.sourceUri shouldBe null
    //        returnedDoc.captureSource shouldBe cdr.captureSource
    //        returnedDoc.uri shouldBe cdr.uri
    //        returnedDoc.team shouldBe cdr.team
    //        returnedDoc.extractedMetadata shouldBe null
    //        returnedDoc.categories shouldBe null
    //        returnedDoc.annotations shouldBe cdr.annotations
    //
    //        mockServer.shutdown()
    //    }
    //
    //    "ElasticsearchService.getDocument" should "return a cdr with only fields in fieldsIncl minus those in fieldsExcl that overlap" in {
    //        val mockServer : MockWebServer = new MockWebServer()
    //        val mockResponse = new MockResponse()
    //          .setResponseCode( 200 )
    //          .setBody( Resource.getAsString( "test_es_cdr.json" ) )
    //        mockServer.enqueue( mockResponse )
    //        mockServer.start( 8097 )
    //
    //        val returnedDoc : DartCdrDocumentDto = esService.getDocument( "fake_doc_id", Some( "document_id,extracted_metadata.Title ,extracted_metadata.Pages, source_uri " ),
    //        Some( "source_uri, extracted_metadata.Pages,annotations" ) )
    //        val cdr =  Mapper.unmarshal( Resource.getAsString( "test_cdr.json" ), classOf[ DartCdrDocumentDto ] )
    //
    //        returnedDoc.documentId shouldBe cdr.documentId
    //        returnedDoc.timestamp shouldBe null
    //        returnedDoc.contentType shouldBe null
    //        returnedDoc.extractedText shouldBe null
    //        returnedDoc.sourceUri shouldBe null
    //        returnedDoc.captureSource shouldBe null
    //        returnedDoc.uri shouldBe null
    //        returnedDoc.team shouldBe null
    //        returnedDoc.extractedMetadata.language shouldBe null
    //        returnedDoc.extractedMetadata.classification shouldBe null
    //        returnedDoc.extractedMetadata.docType shouldBe null
    //        returnedDoc.extractedMetadata.url shouldBe null
    //        returnedDoc.extractedMetadata.description shouldBe null
    //        returnedDoc.extractedMetadata.creationDate shouldBe null
    //        returnedDoc.extractedMetadata.title shouldBe cdr.extractedMetadata.title
    //        returnedDoc.extractedMetadata.pages shouldBe null
    //        returnedDoc.extractedMetadata.author shouldBe null
    //        returnedDoc.extractedMetadata.creator shouldBe null
    //        returnedDoc.extractedMetadata.modificationDate shouldBe null
    //        returnedDoc.extractedMetadata.producer shouldBe null
    //        returnedDoc.extractedMetadata.publisher shouldBe null
    //        returnedDoc.extractedMetadata.subject shouldBe null
    //        returnedDoc.categories shouldBe null
    //        returnedDoc.annotations shouldBe null
    //
    //        mockServer.shutdown()
    //    }

//    val mockSmallRes : String =
//        """{
//          |  "_scroll_id": "IUHiSURHGkjsBFdskjfkEUB",
//          |  "took": 15,
//          |  "timed_out": false,
//          |  "_shards": {
//          |    "total": 5,
//          |    "successful": 5,
//          |    "skipped": 0,
//          |    "failed": 0
//          |  },
//          |  "hits": {
//          |    "total": 10,
//          |    "max_score": 0.27045804,
//          |    "hits": [
//          |      {
//          |        "_index": "cdr_search",
//          |        "_type": "CDR_Document",
//          |        "_id": "4ed5bea9d2f4cadfa8a9d215dded5dc3",
//          |        "_score": 0.27045804,
//          |        "_source": {
//          |          "extracted_metadata": {
//          |            "Title": "Ambassadors extend New Year wishes, hoping for strengthened bilateral ties"
//          |          },
//          |          "document_id": "4ed5bea9d2f4cadfa8a9d215dded5dc3"
//          |        }
//          |      },
//          |      {
//          |        "_index": "cdr_search",
//          |        "_type": "CDR_Document",
//          |        "_id": "800bb5a15b5c9ced02db3df026ecca3b",
//          |        "_score": 0.26871628,
//          |        "_source": {
//          |          "extracted_metadata": {
//          |            "Title": "Ambassador Berhane Receives Copy of the Credentials of the Newly-Appointed Ambassador of Canada"
//          |          },
//          |          "document_id": "800bb5a15b5c9ced02db3df026ecca3b"
//          |        }
//          |      },
//          |      {
//          |        "_index": "cdr_search",
//          |        "_type": "CDR_Document",
//          |        "_id": "993506a5679b229901639e96048f07a6",
//          |        "_score": 0.26841372,
//          |        "_source": {
//          |          "extracted_metadata": {
//          |            "Title": "Former US Diplomat Calls for Free, Fair Elections in Ethiopia"
//          |          },
//          |          "document_id": "993506a5679b229901639e96048f07a6"
//          |        }
//          |      },
//          |      {
//          |        "_index": "cdr_search",
//          |        "_type": "CDR_Document",
//          |        "_id": "f6a356221930082a50b56d103f879af3",
//          |        "_score": 0.26795644,
//          |        "_source": {
//          |          "extracted_metadata": {
//          |            "Title": "President Mulatu Receives Credentials of Eight Ambassadors"
//          |          },
//          |          "document_id": "f6a356221930082a50b56d103f879af3"
//          |        }
//          |      },
//          |      {
//          |        "_index": "cdr_search",
//          |        "_type": "CDR_Document",
//          |        "_id": "fbfa01bbf857b9e7e3284b781426278f",
//          |        "_score": 0.26744416,
//          |        "_source": {
//          |          "extracted_metadata": {
//          |            "Title": "Turkey to invest in Ethiopias hydropower"
//          |          },
//          |          "document_id": "fbfa01bbf857b9e7e3284b781426278f"
//          |        }
//          |      },
//          |      {
//          |        "_index": "cdr_search",
//          |        "_type": "CDR_Document",
//          |        "_id": "6c6486a08ed079ef985fac23f13279e1",
//          |        "_score": 0.2673608,
//          |        "_source": {
//          |          "extracted_metadata": {
//          |            "Title": "Swedish H&M Looks Ethiopian Textile Industry"
//          |          },
//          |          "document_id": "6c6486a08ed079ef985fac23f13279e1"
//          |        }
//          |      },
//          |      {
//          |        "_index": "cdr_search",
//          |        "_type": "CDR_Document",
//          |        "_id": "3de48fbc70241db3e57c794f28948ff5",
//          |        "_score": 0.2672313,
//          |        "_source": {
//          |          "extracted_metadata": {
//          |            "Title": "Ethiopia Economic Update – Overcoming Inflation, Raising Competitiveness"
//          |          },
//          |          "document_id": "3de48fbc70241db3e57c794f28948ff5"
//          |        }
//          |      },
//          |      {
//          |        "_index": "cdr_search",
//          |        "_type": "CDR_Document",
//          |        "_id": "2c0a670344da82a9130d4e8d98134afe",
//          |        "_score": 0.26704112,
//          |        "_source": {
//          |          "extracted_metadata": {
//          |            "Title": "Ethiopia rebukes Human Rights Watch over letter to UK foreign secretary"
//          |          },
//          |          "document_id": "2c0a670344da82a9130d4e8d98134afe"
//          |        }
//          |      },
//          |      {
//          |        "_index": "cdr_search",
//          |        "_type": "CDR_Document",
//          |        "_id": "9468d075a7d8288376ef181f2a44625e",
//          |        "_score": 0.2669899,
//          |        "_source": {
//          |          "extracted_metadata": {
//          |            "Title": "China, Ethiopia sign loan agreement"
//          |          },
//          |          "document_id": "9468d075a7d8288376ef181f2a44625e"
//          |        }
//          |      },
//          |      {
//          |        "_index": "cdr_search",
//          |        "_type": "CDR_Document",
//          |        "_id": "0822b6993084e7748405d308015c4ad1",
//          |        "_score": 0.2668702,
//          |        "_source": {
//          |          "extracted_metadata": {
//          |            "Title": "Ethiopia: China's successful little brother"
//          |          },
//          |          "document_id": "0822b6993084e7748405d308015c4ad1"
//          |        }
//          |      }
//          |    ]
//          |  }
//          |}""".stripMargin
//
//    val scrollRes =
//        """{
//          |  "_scroll_id": "IUHiSURHGkjsBFdskjfkEUB",
//          |  "took": 15,
//          |  "timed_out": false,
//          |  "_shards": {
//          |    "total": 5,
//          |    "successful": 5,
//          |    "skipped": 0,
//          |    "failed": 0
//          |  },
//          |  "hits": {
//          |    "total": 0,
//          |    "hits": []
//          |  }
//          |}""".stripMargin
//
//    "ElasticsearchService.searchGetAll" should "return a map of doc-ids to es-scores" in {
//
//        val mockServer : MockWebServer = new MockWebServer()
//        val mockResponse = new MockResponse()
//          .setResponseCode( 200 )
//          .setBody( mockSmallRes )
//        mockServer.enqueue( mockResponse )
//        val scrollResponse = new MockResponse()
//            .setResponseCode( 200 )
//            .setBody( scrollRes )
//        mockServer.enqueue( scrollResponse )
//        val scrollEndResponse = new MockResponse()
//            .setResponseCode( 200 )
//        mockServer.enqueue( scrollEndResponse )
//        mockServer.start( 8097 )
//
//        val sr = esService.searchGetAll( "test query" )
//
//        sr.isDefined shouldBe true
//
//        val docMap = sr.get
//
//        docMap.size shouldBe 10
//
//        docMap.contains("4ed5bea9d2f4cadfa8a9d215dded5dc3") shouldBe true
//        docMap("4ed5bea9d2f4cadfa8a9d215dded5dc3") shouldBe 0.27045804
//
//        docMap.contains("800bb5a15b5c9ced02db3df026ecca3b") shouldBe true
//        docMap("800bb5a15b5c9ced02db3df026ecca3b") shouldBe 0.26871628
//
//        docMap.contains("0822b6993084e7748405d308015c4ad1") shouldBe true
//        docMap("0822b6993084e7748405d308015c4ad1") shouldBe 0.2668702
//
//        docMap.contains("9468d075a7d8288376ef181f2a44625e") shouldBe true
//        docMap("9468d075a7d8288376ef181f2a44625e") shouldBe 0.2669899
//
//        docMap.contains("2c0a670344da82a9130d4e8d98134afe") shouldBe true
//        docMap("2c0a670344da82a9130d4e8d98134afe") shouldBe 0.26704112
//
//        docMap.contains("3de48fbc70241db3e57c794f28948ff5") shouldBe true
//        docMap("3de48fbc70241db3e57c794f28948ff5") shouldBe 0.2672313
//
//        docMap.contains("6c6486a08ed079ef985fac23f13279e1") shouldBe true
//        docMap("6c6486a08ed079ef985fac23f13279e1") shouldBe 0.2673608
//
//        docMap.contains("fbfa01bbf857b9e7e3284b781426278f") shouldBe true
//        docMap("fbfa01bbf857b9e7e3284b781426278f") shouldBe 0.26744416
//
//        docMap.contains("f6a356221930082a50b56d103f879af3") shouldBe true
//        docMap("f6a356221930082a50b56d103f879af3") shouldBe 0.26795644
//
//        docMap.contains("993506a5679b229901639e96048f07a6") shouldBe true
//        docMap("993506a5679b229901639e96048f07a6") shouldBe 0.26841372
//
//        mockServer.shutdown()
//
//    }
}
