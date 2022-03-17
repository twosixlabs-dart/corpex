package com.twosixlabs.dart.corpex.services.es.models

import com.twosixlabs.dart.corpex.api.tools.Mapper
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class EsSearchResponseTest extends AnyFlatSpecLike with Matchers {

    "EsResponseTest" should "unmarshall a valid ES response (including stored fields)" in {
        val esResponseJson =
            """{
              |  "took": 7,
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
              |        "fields": {
              |          "extracted_text.length": [
              |            531
              |          ],
              |          "other_field": [
              |            "some string"
              |          ],
              |          "third_field": [
              |            0.9824
              |          ]
              |        },
              |        "_source": {
              |          "extracted_metadata": {
              |            "Pages": 0
              |          }
              |        }
              |      }
              |    ]
              |  }
              |}""".stripMargin

        val esResponse = Mapper.unmarshal( esResponseJson, classOf[ EsSearchResponse ] )

        esResponse.hits.hits.length shouldBe 1
        esResponse.hits.hits.head.index shouldBe "cdr_search"
        esResponse.hits.hits.head.hitType shouldBe "CDR_Document"
        esResponse.hits.hits.head.id shouldBe "4ed5bea9d2f4cadfa8a9d215dded5dc3"
        esResponse.hits.hits.head.score shouldBe 0.27045804
        esResponse.hits.hits.head.result.cdr.extractedMetadata.pages shouldBe 0
        esResponse.hits.hits.head.fields.get( "extracted_text.length" ).head.long.get shouldBe 531
        esResponse.hits.hits.head.fields.get( "other_field" ).head.string.get shouldBe "some string"
        esResponse.hits.hits.head.fields.get( "third_field" ).head.double.get shouldBe 0.9824
        esResponse.hits.maxScore shouldBe 0.27045804
        esResponse.hits.total.exact shouldBe true
        esResponse.hits.total.total shouldBe 38989
        esResponse.took shouldBe 7
        esResponse.timedOut shouldBe false

    }

    "EsResponseTest" should "unmarshall a valid ES response when the total field is an object" in {
        val esResponseJson =
            """{
              |  "took": 7,
              |  "timed_out": false,
              |  "_shards": {
              |    "total": 5,
              |    "successful": 5,
              |    "skipped": 0,
              |    "failed": 0
              |  },
              |  "hits": {
              |    "total": {
              |      "value": 10000,
              |      "relation": "eq"
              |    },
              |    "max_score": 0.27045804,
              |    "hits": [
              |      {
              |        "_index": "cdr_search",
              |        "_type": "CDR_Document",
              |        "_id": "4ed5bea9d2f4cadfa8a9d215dded5dc3",
              |        "_score": 0.27045804,
              |        "_source": {
              |          "extracted_metadata": {
              |            "Pages": 0
              |          }
              |        }
              |      }
              |    ]
              |  }
              |}""".stripMargin

        val esResponse = Mapper.unmarshal( esResponseJson, classOf[ EsSearchResponse ] )

        esResponse.hits.hits.length shouldBe 1
        esResponse.hits.hits.head.index shouldBe "cdr_search"
        esResponse.hits.hits.head.hitType shouldBe "CDR_Document"
        esResponse.hits.hits.head.id shouldBe "4ed5bea9d2f4cadfa8a9d215dded5dc3"
        esResponse.hits.hits.head.score shouldBe 0.27045804
        esResponse.hits.hits.head.result.cdr.extractedMetadata.pages shouldBe 0
        esResponse.hits.maxScore shouldBe 0.27045804
        esResponse.hits.total.exact shouldBe true
        esResponse.hits.total.total shouldBe 10000
        esResponse.took shouldBe 7
        esResponse.timedOut shouldBe false

    }

    "EsResponseTest" should "unmarshall a valid ES response when the total field is an object with a gte relation" in {
        val esResponseJson =
            """{
              |  "took": 7,
              |  "timed_out": false,
              |  "_shards": {
              |    "total": 5,
              |    "successful": 5,
              |    "skipped": 0,
              |    "failed": 0
              |  },
              |  "hits": {
              |    "total": {
              |      "value": 10000,
              |      "relation": "gte"
              |    },
              |    "max_score": 0.27045804,
              |    "hits": [
              |      {
              |        "_index": "cdr_search",
              |        "_type": "CDR_Document",
              |        "_id": "4ed5bea9d2f4cadfa8a9d215dded5dc3",
              |        "_score": 0.27045804,
              |        "_source": {
              |          "extracted_metadata": {
              |            "Pages": 0
              |          }
              |        }
              |      }
              |    ]
              |  }
              |}""".stripMargin

        val esResponse = Mapper.unmarshal( esResponseJson, classOf[ EsSearchResponse ] )

        esResponse.hits.hits.length shouldBe 1
        esResponse.hits.hits.head.index shouldBe "cdr_search"
        esResponse.hits.hits.head.hitType shouldBe "CDR_Document"
        esResponse.hits.hits.head.id shouldBe "4ed5bea9d2f4cadfa8a9d215dded5dc3"
        esResponse.hits.hits.head.score shouldBe 0.27045804
        esResponse.hits.hits.head.result.cdr.extractedMetadata.pages shouldBe 0
        esResponse.hits.maxScore shouldBe 0.27045804
        esResponse.hits.total.exact shouldBe false
        esResponse.hits.total.total shouldBe 10000
        esResponse.took shouldBe 7
        esResponse.timedOut shouldBe false

    }

}
