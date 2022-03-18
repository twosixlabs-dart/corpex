package com.twosixlabs.dart.corpex.controller

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.groups.{ ProgramManager, TenantGroup }
import com.twosixlabs.dart.auth.tenant.{ CorpusTenant, Leader, ReadOnly }
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.commons.config.StandardCliConfig
import annotations.WipTest
import com.twosixlabs.dart.corpex.services.search.SearchService
import com.twosixlabs.dart.corpex.services.search.es.ElasticsearchSearchService
import com.typesafe.config.{ Config, ConfigFactory }
import okhttp3.mockwebserver.{ MockResponse, MockWebServer }
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatra.test.scalatest.ScalatraSuite

import javax.servlet.http.HttpServletRequest
import scala.collection.JavaConverters._

class SearchControllerAuthTest extends AnyFlatSpecLike with ScalatraSuite with Matchers with MockFactory with StandardCliConfig {

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

    val tenant1 : CorpusTenant = CorpusTenant( "tenant-1" )
    val tenant2 : CorpusTenant = CorpusTenant( "tenant-2" )

    val config : Config = ConfigFactory.load( "test" ).resolve()

    val baseDependencies = SecureDartController.deps( "corpex", config )

    val searchControllerDeps = new SearchController.Dependencies {
        override val searchService : SearchService = ElasticsearchSearchService( config )
        override val serviceName : String = baseDependencies.serviceName
        override val secretKey : Option[String ] = baseDependencies.secretKey
        override val useDartAuth : Boolean = true
        override val basicAuthCredentials : Seq[ (String, String) ] = Nil
    }

    addServlet( new SearchController( searchControllerDeps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser = {
            DartUser( "test", Set( TenantGroup( tenant1, Leader ), TenantGroup(  tenant2, ReadOnly ) ) )
        }
    }, "/search/*" )

    "POST to /search" should "return 200 with valid json response for a valid request when tenant_id has a tenant id authorized for the user" in {
        Thread.sleep( 1000 )
        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse = new MockResponse()
          .setResponseCode( 200 )
          .setBody( mockEsJson )
        mockServer.enqueue( mockResponse )
        mockServer.start( 8096 )

        post( "/search", body = s"""{"page":0,"page_size":10,"queries":[{"bool_type":"MUST","query_type":"TEXT","queried_fields":["cdr.extracted_text"],"query_string":"full text search"}],"tenant_id":"${tenant1.id}"}""" ) {
            mockServer.shutdown()
            response.body shouldBe """{"num_results":38989,"exact_num":true,"page":0,"num_pages":3899,"page_size":10,"results":[{"es_score":0.27045804,"cdr":{"extracted_metadata":{"Title":"Ambassadors extend New Year wishes, hoping for strengthened bilateral ties"},"document_id":"4ed5bea9d2f4cadfa8a9d215dded5dc3","annotations":[]}},{"es_score":0.26871628,"cdr":{"extracted_metadata":{"Title":"Ambassador Berhane Receives Copy of the Credentials of the Newly-Appointed Ambassador of Canada"},"document_id":"800bb5a15b5c9ced02db3df026ecca3b","annotations":[]}},{"es_score":0.26841372,"cdr":{"extracted_metadata":{"Title":"Former US Diplomat Calls for Free, Fair Elections in Ethiopia"},"document_id":"993506a5679b229901639e96048f07a6","annotations":[]}},{"es_score":0.26795644,"cdr":{"extracted_metadata":{"Title":"President Mulatu Receives Credentials of Eight Ambassadors"},"document_id":"f6a356221930082a50b56d103f879af3","annotations":[]}},{"es_score":0.26744416,"cdr":{"extracted_metadata":{"Title":"Turkey to invest in Ethiopias hydropower"},"document_id":"fbfa01bbf857b9e7e3284b781426278f","annotations":[]}},{"es_score":0.2673608,"cdr":{"extracted_metadata":{"Title":"Swedish H&M Looks Ethiopian Textile Industry"},"document_id":"6c6486a08ed079ef985fac23f13279e1","annotations":[]}},{"es_score":0.2672313,"cdr":{"extracted_metadata":{"Title":"Ethiopia Economic Update – Overcoming Inflation, Raising Competitiveness"},"document_id":"3de48fbc70241db3e57c794f28948ff5","annotations":[]}},{"es_score":0.26704112,"cdr":{"extracted_metadata":{"Title":"Ethiopia rebukes Human Rights Watch over letter to UK foreign secretary"},"document_id":"2c0a670344da82a9130d4e8d98134afe","annotations":[]}},{"es_score":0.2669899,"cdr":{"extracted_metadata":{"Title":"China, Ethiopia sign loan agreement"},"document_id":"9468d075a7d8288376ef181f2a44625e","annotations":[]}},{"es_score":0.2668702,"cdr":{"extracted_metadata":{"Title":"Ethiopia: China's successful little brother"},"document_id":"0822b6993084e7748405d308015c4ad1","annotations":[]}}]}"""
            status shouldBe 200
        }
    }

    "POST to /search" should "return 403 with valid error message for a valid request when tenant_id has a tenant id not authorized for the user" in {

        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse = new MockResponse()
          .setResponseCode( 200 )
          .setBody( mockEsJson )
        mockServer.enqueue( mockResponse )
        mockServer.start( 8096 )

        post( "/search", body = s"""{"page":0,"page_size":10,"queries":[{"bool_type":"MUST","query_type":"TEXT","queried_fields":["cdr.extracted_text"],"query_string":"full text search"}],"tenant_id":"unauthorized-tenant"}""" ) {
            mockServer.shutdown()
            response.body should ( include( """"status":403""" ) and include( """"Operation not authorized: \n\tUnpermitted operation: SearchCorpus on tenant:""" ) )
            status shouldBe 403
        }
    }

    val mockEsCountJson =
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

    "POST to /search/count" should "return 200 with valid json count response for a valid request when tenant_id has a tenant authorized for the user" in {
        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse = new MockResponse()
          .setResponseCode( 200 )
          .setBody( mockEsCountJson )
        mockServer.enqueue( mockResponse )
        mockServer.start( 8096 )

        post( "/search/count", body = s"""{"queries":[{"bool_type":"MUST","query_type":"TEXT","queried_fields":["cdr.extracted_text"],"query_string":"full text search"}],"tenant_id":"${tenant2.id}"}""" ) {
            mockServer.shutdown()
            response.body shouldBe """{"num_results":44030,"exact_num":true}"""
            status shouldBe 200
        }
    }

    "POST to /search/count" should "return 403 with valid error response for a valid request when tenant_id has a tenant not authorized for the user" in {
        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse = new MockResponse()
          .setResponseCode( 200 )
          .setBody( mockEsCountJson )
        mockServer.enqueue( mockResponse )
        mockServer.start( 8096 )

        post( "/search/count", body = s"""{"queries":[{"bool_type":"MUST","query_type":"TEXT","queried_fields":["cdr.extracted_text"],"query_string":"full text search"}],"tenant_id":"unauthorized-tenant"}""" ) {
            mockServer.shutdown()
            response.body should ( include( """"status":403""" ) and include( """"Operation not authorized: \n\tUnpermitted operation: SearchCorpus on tenant:""" ) )
            status shouldBe 403
        }
    }

    val mockEsScrollStartJson =
        """
          |{
          |  "_scroll_id": "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAAAAkWb05wTkJzSTFUSjJUZ3dCOVZ6NXB2Zw==",
          |  "took": 12,
          |  "timed_out": false,
          |  "_shards": {
          |    "total": 1,
          |    "successful": 1,
          |    "skipped": 0,
          |    "failed": 0
          |  },
          |  "hits": {
          |    "total": {
          |      "value": 15,
          |      "relation": "eq"
          |    },
          |    "max_score": 1.0,
          |    "hits": [
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "d5c413df85be46e6d27d0e7ba027858f",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "d5c413df85be46e6d27d0e7ba027858f"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "df7be300058ea8df8f4701670284c271",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "df7be300058ea8df8f4701670284c271"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "2b32f4a87d57ceb1a671b0d51f909453",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "2b32f4a87d57ceb1a671b0d51f909453"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "5c43bb6c9df2f2238abfccd61506c985",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "5c43bb6c9df2f2238abfccd61506c985"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "2d79f269f0010ffbeaf26533184dfc14",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "2d79f269f0010ffbeaf26533184dfc14"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "0571770d3fc7573e515ac361e4b942c9",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "0571770d3fc7573e515ac361e4b942c9"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "434e9d2ef2e95605e70d12e8a78c6aea",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "434e9d2ef2e95605e70d12e8a78c6aea"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "37ff939e43a2297b1ab9bb5b06d4e403",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "37ff939e43a2297b1ab9bb5b06d4e403"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "9243b122e7cc2e7447cdd87500d5f197",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "9243b122e7cc2e7447cdd87500d5f197"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "634f221da953e225ff9669bd620415d1",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "634f221da953e225ff9669bd620415d1"
          |        }
          |      }
          |    ]
          |  }
          |}
          |""".stripMargin

    val mockEsScrollContinueJson =
        """
          |{
          |  "_scroll_id": "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAAAAoWb05wTkJzSTFUSjJUZ3dCOVZ6NXB2Zw==",
          |  "took": 14,
          |  "timed_out": false,
          |  "terminated_early": true,
          |  "_shards": {
          |    "total": 1,
          |    "successful": 1,
          |    "skipped": 0,
          |    "failed": 0
          |  },
          |  "hits": {
          |    "total": {
          |      "value": 15,
          |      "relation": "eq"
          |    },
          |    "max_score": 1.0,
          |    "hits": [
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "206ef8242affbb2f0b5b9ac86788b1c8",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "206ef8242affbb2f0b5b9ac86788b1c8"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "1d6edda33a134599943ef01361f15008",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "1d6edda33a134599943ef01361f15008"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "49b3e245cc36370aef84bb89b605b9c0",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "49b3e245cc36370aef84bb89b605b9c0"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "e7f51fc9edbe2c25bf502ddd636da770",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "e7f51fc9edbe2c25bf502ddd636da770"
          |        }
          |      },
          |      {
          |        "_index": "cdr_search",
          |        "_type": "_doc",
          |        "_id": "84b35b53c4efe94966f42fd67a02c73c",
          |        "_score": 1.0,
          |        "_source": {
          |          "document_id": "84b35b53c4efe94966f42fd67a02c73c"
          |        }
          |      }
          |    ]
          |  }
          |}
          |""".stripMargin

    val esMockScrollEndedJson =
        """
          |{
          |  "_scroll_id": "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAAAAoWb05wTkJzSTFUSjJUZ3dCOVZ6NXB2Zw==",
          |  "took": 14,
          |  "timed_out": false,
          |  "terminated_early": true,
          |  "_shards": {
          |    "total": 1,
          |    "successful": 1,
          |    "skipped": 0,
          |    "failed": 0
          |  },
          |  "hits": {
          |    "total": {
          |      "value": 15,
          |      "relation": "eq"
          |    },
          |    "max_score": 1.0,
          |    "hits": []
          |  }
          |}
          |""".stripMargin

    val mockShaveResults = List("d5c413df85be46e6d27d0e7ba027858f","df7be300058ea8df8f4701670284c271","2b32f4a87d57ceb1a671b0d51f909453","5c43bb6c9df2f2238abfccd61506c985","2d79f269f0010ffbeaf26533184dfc14","0571770d3fc7573e515ac361e4b942c9","434e9d2ef2e95605e70d12e8a78c6aea","37ff939e43a2297b1ab9bb5b06d4e403","9243b122e7cc2e7447cdd87500d5f197","634f221da953e225ff9669bd620415d1","206ef8242affbb2f0b5b9ac86788b1c8","1d6edda33a134599943ef01361f15008","49b3e245cc36370aef84bb89b605b9c0","e7f51fc9edbe2c25bf502ddd636da770","84b35b53c4efe94966f42fd67a02c73c")

    "POST to /search/shave" should "return 200 and return a json list of doc ids of length equal to take query parameter if that is less than the number of results when tenant_id has tenant authorized for user" in {
        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse1 = new MockResponse()
          .setResponseCode( 200 )
          .setBody( mockEsScrollStartJson )
        val mockResponse2 = new MockResponse()
          .setResponseCode( 200 )
          .setBody( mockEsScrollContinueJson )
        val mockResponse3 = new MockResponse()
          .setResponseCode( 200 )
          .setBody( esMockScrollEndedJson )
        mockServer.enqueue( mockResponse1 )
        mockServer.enqueue( mockResponse2 )
        mockServer.enqueue( mockResponse3 )
        mockServer.start( 8096 )

        post( "/search/shave?take=13", body = s"""{"queries":[{"bool_type":"MUST","query_type":"TEXT","queried_fields":["cdr.extracted_text"],"query_string":"full text search"}],"tenant_id":"${tenant1.id}"}""" ) {
            mockServer.shutdown()
            response.body shouldBe """["d5c413df85be46e6d27d0e7ba027858f","df7be300058ea8df8f4701670284c271","2b32f4a87d57ceb1a671b0d51f909453","5c43bb6c9df2f2238abfccd61506c985","2d79f269f0010ffbeaf26533184dfc14","0571770d3fc7573e515ac361e4b942c9","434e9d2ef2e95605e70d12e8a78c6aea","37ff939e43a2297b1ab9bb5b06d4e403","9243b122e7cc2e7447cdd87500d5f197","634f221da953e225ff9669bd620415d1","206ef8242affbb2f0b5b9ac86788b1c8","1d6edda33a134599943ef01361f15008","49b3e245cc36370aef84bb89b605b9c0"]"""
            status shouldBe 200
        }
    }

    "POST to /search/shave" should "return 403 and return a valid error response if tenant_id has tenant not authorized for user" in {
        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse1 = new MockResponse()
          .setResponseCode( 200 )
          .setBody( mockEsScrollStartJson )
        val mockResponse2 = new MockResponse()
          .setResponseCode( 200 )
          .setBody( mockEsScrollContinueJson )
        val mockResponse3 = new MockResponse()
          .setResponseCode( 200 )
          .setBody( esMockScrollEndedJson )
        mockServer.enqueue( mockResponse1 )
        mockServer.enqueue( mockResponse2 )
        mockServer.enqueue( mockResponse3 )
        mockServer.start( 8096 )

        post( "/search/shave?take=13", body = s"""{"queries":[{"bool_type":"MUST","query_type":"TEXT","queried_fields":["cdr.extracted_text"],"query_string":"full text search"}],"tenant_id":"unauthorized-tenant"}""" ) {
            mockServer.shutdown()
            response.body should ( include( """"status":403""" ) and include( """"Operation not authorized: \n\tUnpermitted operation: SearchCorpus on tenant:""" ) )
            status shouldBe 403
        }
    }

    override def header = null
}
