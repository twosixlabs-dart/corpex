package com.twosixlabs.dart.corpex.api.models

import com.twosixlabs.dart.corpex.api.configuration.annotations.{EntityTagTypes, TagIds}
import com.twosixlabs.dart.corpex.api.enums._
import com.twosixlabs.dart.corpex.api.exceptions.{InvalidAggQueryException, InvalidRequestException, InvalidSearchQueryException}
import com.twosixlabs.dart.corpex.api.models.queries.{CorpexBoolQuery, CorpexCdrDateQuery, CorpexFacetAggQuery, CorpexFacetQuery, CorpexFieldAggQuery, CorpexIntegerQuery, CorpexTagDateQuery, CorpexTagQuery, CorpexTagTypesAggQuery, CorpexTagValuesAggQuery, CorpexTermQuery, CorpexTextQuery}
import com.twosixlabs.dart.corpex.api.tools.Mapper
import org.scalatest.{FlatSpecLike, Matchers}

import scala.util.{Failure, Success, Try}

class CorpexSearchRequestTest extends FlatSpecLike with Matchers {

    behavior of "CorpexSearchRequest"

    it should "marshal json correctly for a full text search request" in {
        val query = CorpexTextQuery( boolType = BoolType.MUST, queriedFields = List( "cdr.extracted_text" ), queryString = "some query" )
        val req = CorpexSearchRequest( queries = Some( List( query ) ), pageSize = Some( 10 ), page = Some( 0 ) )

        val json = Mapper.marshal( req )

        json shouldBe """{"page":0,"page_size":10,"queries":[{"query_type":"TEXT","bool_type":"MUST","queried_fields":["cdr.extracted_text"],"query_string":"some query"}]}"""
    }

    it should "unmarshal json correctly from a full text search request" in {
        val json =
            """{"page":3,"page_size":50,"queries":[{"bool_type":"FILTER","query_type":"TEXT","queried_fields":["cdr.extracted_text"],"query_string":"full -text +-search +\"query string\""}]}"""
        val req = Mapper.unmarshal( json, classOf[ CorpexSearchRequest ] )

        req.page.get shouldBe 3
        req.pageSize.get shouldBe 50
        req.queries.get.length shouldBe 1
        req.queries.get.head.boolType shouldBe BoolType.FILTER
        val query = req.queries.get.head.asInstanceOf[ CorpexTextQuery ]
        query.queriedFields shouldBe List( "cdr.extracted_text" )
        query.queryString shouldBe """full -text +-search +"query string""""
    }

    it should "marshal json correctly for a complex search with a bool sub-query" in {
        val subquery1 = CorpexTextQuery( boolType = BoolType.MUST,
                                         queryString = "test query",
                                         queriedFields = List( "cdr.extracted_text" ) )
        val subquery2 = CorpexTagQuery( boolType = BoolType.MUST,
                                        tagId = TagIds.QNTFY_NER.apiLabel,
                                        tagTypes = Some( List( EntityTagTypes.ORG.apiLabel ) ),
                                        tagValues = Some( List( "Washington" ) ) )
        val query1 = CorpexCdrDateQuery( boolType = BoolType.FILTER,
                                         queriedFields = List( "cdr.extracted_metadata.CreationDate" ),
                                         dateHi = MultiValue( long = Some( 3485398457L ) ),
                                         dateLo = MultiValue( long = Some( 3459739485L ) ) )
        val query2 = CorpexBoolQuery( boolType = BoolType.SHOULD,
                                      queries = List( subquery1, subquery2 ) )
        val req = CorpexSearchRequest( page = Some( 0 ),
                                       pageSize = Some( 10 ),
                                       queries = Some( List( query1, query2 ) ) )

        val json = Mapper.marshal( req )
        json shouldBe
        """{"page":0,"page_size":10,"queries":[{"query_type":"CDR_DATE","bool_type":"FILTER","queried_fields":["cdr.extracted_metadata.CreationDate"],"date_hi":3485398457,"date_lo":3459739485},{"query_type":"BOOL","bool_type":"SHOULD","queries":[{"query_type":"TEXT","bool_type":"MUST","queried_fields":["cdr.extracted_text"],"query_string":"test query"},{"query_type":"TAG","bool_type":"MUST","tag_id":"qntfy-ner","tag_types":["ORG"],"tag_values":["Washington"]}]}]}""".stripMargin
    }

    it should "throw an appropriate exception when unmarshalling json with unknown fields" in {
        val json = """{"unknown_field":500,"page":0,"page_size":10,"queries":[{"query_type":"TEXT","bool_type":"MUST","queried_fields":["cdr.extracted_text"],"query_string":"some query"}]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexSearchRequest ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidRequestException ) => e.getMessage should include ( "invalid field: \"unknown_field" )
            case Failure( e : Throwable ) =>
                fail( s"threw exception other than InvalidSearchQueryException: ${e.getClass} -- ${e.getMessage}" )
        }
    }

    behavior of "CorpexSearchQuery"

    it should "throw an appropriate error when marshalling json where query_type is missing" in {
        val json =
            """{"bool_type":"FILTER","queried_fields":["cdr.extracted_text"],"query_string":"full -text +-search +\"query string\""}]}"""
        Try ( Mapper.unmarshal( json, classOf[ CorpexTextQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidRequestException ) => e.getMessage should include ( "missing query_type" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when marshalling json where query_type is invalid" in {
        val json =
            """{"query_type":"INVALID_VALUE","bool_type":"FILTER","queried_fields":["cdr.extracted_text"],"query_string":"full -text +-search +\"query string\""}]}"""
        Try ( Mapper.unmarshal( json, classOf[ CorpexTextQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidRequestException ) => e.getMessage should include ( "invalid query type: INVALID_VALUE" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an error when marshalling TEXT query json when bool_type is missing" in {
        val json =
            """{"query_type":"TEXT","queried_fields":["cdr.extracted_text"],"query_string":"full -text +-search +\"query string\""}]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTextQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "missing required field: bool_type" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an error when marshalling TEXT query json when queried fields is missing" in {
        val json =
            """{"query_type":"TEXT","bool_type":"MUST","query_string":"full -text +-search +\"query string\""}]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTextQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "TEXT query missing required field: queried_fields" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an error when marshalling TEXT query json when queried fields is wrong type" in {
        val json =
            """{"query_type":"TEXT","bool_type":"MUST","queried_fields":"cdr.extracted_text","query_string":"full -text +-search +\"query string\""}]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTextQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidRequestException ) => e.getMessage should ( include ( "invalid field type at line: " ) and include( ", column: " ) )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    behavior of "CorpexTextQuery"

    it should "throw an appropriate error when query_string is absent" in {
        val json = """{"query_type":"TEXT","bool_type":"MUST","queried_fields":["cdr.extracted_text"]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTextQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "TEXT query missing required field: query_string" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    behavior of "CorpexTermQuery"

    it should "throw an appropriate error when values_bool_type has wrong boolType, but not when it has supported boolType" in {
        val json = """{"query_type":"TERM","bool_type":"MUST","queried_field":"cdr.content_type","term_values":["some value","second value"],"values_bool_type":"MUST"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTermQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) =>
                if (e.getCause != null) fail( e.getCause.getMessage )
                e.getMessage should include ( "TERM query values_bool_type field cannot have value MUST: supported values are SHOULD or MUST_NOT" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }

        val json1 = """{"query_type":"TERM","bool_type":"MUST","queried_field":"cdr.content_type","term_values":["some value, second value"],"values_bool_type":"SHOULD"}"""

        val res = Mapper.unmarshal( json1, classOf[ CorpexTermQuery ] )
        res.valuesBoolType shouldBe Some( BoolType.SHOULD )
    }

    it should "throw an appropriate error when queried_field is absent" in {
        val json = """{"query_type":"TERM","bool_type":"MUST","term_values":["some value"]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTermQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) =>
                if (e.getCause != null) fail( e.getCause.getMessage )
                e.getMessage should include ( "TERM query missing required field: queried_field" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when queried_field is invalid api value" in {
        val json = """{"query_type":"TERM","bool_type":"MUST","queried_field":"invalid_field","term_values":["some value"]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTermQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "invalid_field is not a valid corpex field" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when term_values is absent" in {
        val json = """{"query_type":"TERM","bool_type":"MUST","queried_field":"cdr.content_type"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTermQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "TERM query missing required field: term_values" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    behavior of "CorpexIntegerQuery"

    it should "throw an appropriate error when queried_fields contain invalid corpex field" in {
        val json = """{"query_type":"INTEGER","bool_type":"MUST","queried_fields":["word_count","invalid_field"],"int_lo":100}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexIntegerQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "invalid_field is not a valid corpex field" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when queried_fields contain valid corpex field but not type INT" in {
        val json = """{"query_type":"INTEGER","bool_type":"MUST","queried_fields":["word_count","cdr.content_type"],"int_lo":100}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexIntegerQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "cdr.content_type is not a valid INTEGER field" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when int_lo and int_hi are undefined" in {
        val json = """{"query_type":"INTEGER","bool_type":"MUST","queried_fields":["word_count"]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexIntegerQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "INTEGER query must include at least one of the fields: int_lo, int_hi" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    behavior of "CorpexCdrDateQuery"

    it should "throw an appropriate error when queried_fields contain valid corpex field but not type DATE" in {
        val json = """{"query_type":"CDR_DATE","bool_type":"MUST","queried_fields":["cdr.extracted_metadata.CreationDate","cdr.content_type"],"date_lo":1000000}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexCdrDateQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) =>
                if (e.getCause != null) {
                    if(e.getCause.getCause != null) fail( e.getCause.getCause.getStackTrace.mkString("\n") )
                    fail( e.getCause.getMessage )
                }

                print( e.getStackTrace.mkString( "\n" ) )
                e.getMessage should include ( "cdr.content_type is not a valid DATE field" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when date_lo and date_hi are both undefined" in {
        val json = """{"query_type":"CDR_DATE","bool_type":"MUST","queried_fields":["cdr.timestamp"]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexCdrDateQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) =>
                if (e.getCause != null) fail( e.getCause.getMessage )
                e.getMessage should include ( "CDR_DATE query must include at least one of the fields: date_lo, date_hi" )
            case Failure( e : Throwable ) =>
                print( e.getMessage )
                e.printStackTrace()
                fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "retrieve OffsetDateTime from epoch milliseconds" in {
        val json = """{"query_type":"CDR_DATE","bool_type":"MUST","queried_fields":["cdr.extracted_metadata.CreationDate"],"date_lo":1591044677000}"""

        val value = Mapper.unmarshal( json, classOf[ CorpexCdrDateQuery ] )
        value.dateHiDate.isDefined shouldBe false
        val loDate = value.dateLoDate.get
        loDate.getYear shouldBe 2020
        loDate.getDayOfMonth shouldBe 1
        loDate.getMonthValue shouldBe 6
        loDate.getHour shouldBe 20
        loDate.getMinute shouldBe 51
        loDate.getSecond shouldBe 17
        loDate.getNano shouldBe 0
    }

    it should "retrieve OffsetDateTime from year string" in {
        val json = """{"query_type":"CDR_DATE","bool_type":"MUST","queried_fields":["cdr.extracted_metadata.CreationDate"],"date_lo":"2020","date_hi":"2020"}"""

        val value = Mapper.unmarshal( json, classOf[ CorpexCdrDateQuery ] )
        val loDate = value.dateLoDate.get
        loDate.getYear shouldBe 2020
        loDate.getDayOfMonth shouldBe 1
        loDate.getMonthValue shouldBe 1
        loDate.getHour shouldBe 0
        loDate.getMinute shouldBe 0
        loDate.getSecond shouldBe 0
        loDate.getNano shouldBe 0
        val hiDate = value.dateHiDate.get
        hiDate.getYear shouldBe 2020
        hiDate.getDayOfMonth shouldBe 31
        hiDate.getMonthValue shouldBe 12
        hiDate.getHour shouldBe 23
        hiDate.getMinute shouldBe 59
        hiDate.getSecond shouldBe 59
        hiDate.getNano shouldBe 999999999
    }

    it should "retrieve OffsetDateTime from year and month string" in {
        val json = """{"query_type":"CDR_DATE","bool_type":"MUST","queried_fields":["cdr.extracted_metadata.CreationDate"],"date_lo":"2020-06","date_hi":"2020-06"}"""

        val value = Mapper.unmarshal( json, classOf[ CorpexCdrDateQuery ] )
        val loDate = value.dateLoDate.get
        loDate.getYear shouldBe 2020
        loDate.getDayOfMonth shouldBe 1
        loDate.getMonthValue shouldBe 6
        loDate.getHour shouldBe 0
        loDate.getMinute shouldBe 0
        loDate.getSecond shouldBe 0
        loDate.getNano shouldBe 0
        val hiDate = value.dateHiDate.get
        hiDate.getYear shouldBe 2020
        hiDate.getDayOfMonth shouldBe 30
        hiDate.getMonthValue shouldBe 6
        hiDate.getHour shouldBe 23
        hiDate.getMinute shouldBe 59
        hiDate.getSecond shouldBe 59
        hiDate.getNano shouldBe 999999999
    }

    it should "retrieve OffsetDateTime from local date string" in {
        val json = """{"query_type":"CDR_DATE","bool_type":"MUST","queried_fields":["cdr.extracted_metadata.CreationDate"],"date_lo":"2020-06-08","date_hi":"2020-06-08"}"""

        val value = Mapper.unmarshal( json, classOf[ CorpexCdrDateQuery ] )
        val loDate = value.dateLoDate.get
        loDate.getYear shouldBe 2020
        loDate.getDayOfMonth shouldBe 8
        loDate.getMonthValue shouldBe 6
        loDate.getHour shouldBe 0
        loDate.getMinute shouldBe 0
        loDate.getSecond shouldBe 0
        loDate.getNano shouldBe 0
        val hiDate = value.dateHiDate.get
        hiDate.getYear shouldBe 2020
        hiDate.getDayOfMonth shouldBe 8
        hiDate.getMonthValue shouldBe 6
        hiDate.getHour shouldBe 23
        hiDate.getMinute shouldBe 59
        hiDate.getSecond shouldBe 59
        hiDate.getNano shouldBe 999999999
    }

    it should "retrieve OffsetDateTime from iso date-time string" in {
        val json = """{"query_type":"CDR_DATE","bool_type":"MUST","queried_fields":["cdr.extracted_metadata.CreationDate"],"date_lo":"2020-06-01T17:12:57.235Z","date_hi":"2020-06-01T17:12:57.235Z"}"""

        val value = Mapper.unmarshal( json, classOf[ CorpexCdrDateQuery ] )
        val loDate = value.dateLoDate.get
        loDate.getYear shouldBe 2020
        loDate.getDayOfMonth shouldBe 1
        loDate.getMonthValue shouldBe 6
        loDate.getHour shouldBe 17
        loDate.getMinute shouldBe 12
        loDate.getSecond shouldBe 57
        loDate.getNano shouldBe 235000000
        val hiDate = value.dateHiDate.get
        hiDate.getYear shouldBe 2020
        hiDate.getDayOfMonth shouldBe 1
        hiDate.getMonthValue shouldBe 6
        hiDate.getHour shouldBe 17
        hiDate.getMinute shouldBe 12
        hiDate.getSecond shouldBe 57
        hiDate.getNano shouldBe 235000000
    }

    behavior of "CorpexTagDateQuery"

    it should "throw an appropriate error when date_lo and date_hi are both undefined" in {
        val json = """{"query_type":"TAG_DATE","bool_type":"MUST"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagDateQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "TAG_DATE query must include at least one of the fields: date_lo, date_hi" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    behavior of "CorpexTagQuery"

    it should "unmarshal json with minimal fields defined" in {
        val json = """{"query_type":"TAG","bool_type":"MUST","tag_id":"qntfy-ner"}"""

        Mapper.unmarshal( json, classOf[ CorpexTagQuery ] )
    }

    it should "throw an appropriate error when tag_id is invalid" in {
        val json = """{"query_type":"TAG","bool_type":"MUST","tag_id":"invalid-tag-id"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "invalid-tag-id is not a valid tag id" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when tag_type is defined but invalid" in {
        val json = """{"query_type":"TAG","bool_type":"MUST","tag_id":"qntfy-ner","tag_types":["PERSON","invalid-tag-type"]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "invalid-tag-type is not a valid tag type" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when tag_types and tag_types_exact are both defined" in {
        val json = """{"query_type":"TAG","bool_type":"MUST","tag_id":"qntfy-ner","tag_types":["PERSON","GPE"],"tag_types_exact":["sdfasd","fdgjhfkjgh"]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "tag_types and tag_types_exact cannot both be defined" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when tag_types is defined along with type_query_string" in {
        val json = """{"query_type":"TAG","bool_type":"MUST","tag_id":"qntfy-ner","tag_types":["PERSON","GPE"],"tag_types_query":"test query"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "tag_types and tag_types_query cannot both be defined" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when tag_types_exact is defined along with type_query_string" in {
        val json = """{"query_type":"TAG","bool_type":"MUST","tag_id":"qntfy-ner","tag_types_exact":["PERSON","GPE"],"tag_types_query":"test query"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "tag_types_exact and tag_types_query cannot both be defined" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when tag_values and tag_values_query are both defined" in {
        val json = """{"query_type":"TAG","bool_type":"MUST","tag_id":"qntfy-ner","tag_values":["val 1","val 2"],"tag_values_query":"test query"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "tag_values and tag_values_query cannot both be defined" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when values_bool_type is defined without tag_values" in {
        val json = """{"query_type":"TAG","bool_type":"MUST","tag_id":"qntfy-ner","values_bool_type":"FILTER"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "values_bool_type is defined without tag_values" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when types_bool_type is defined without tag_types or tag_types_exact" in {
        val json = """{"query_type":"TAG","bool_type":"MUST","tag_id":"qntfy-ner","types_bool_type":"SHOULD"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "types_bool_type is defined without tag_types or tag_types_exact" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "successfully unmarshal request with as many fields as can be defined with valid values" in {
        val json = """{"query_type":"TAG","bool_type":"MUST","tag_id":"qntfy-ner","tag_types":["PERSON","LOC","GPE"],"types_bool_type":"FILTER","tag_values":["val 1","val 2","val 3"],"values_bool_type":"SHOULD"}"""

        Mapper.unmarshal( json, classOf[ CorpexTagQuery ] )
    }

    behavior of "CorpexFacetQuery"

    it should "unmarshal json with minimal fields defined" in {
        val json = """{"query_type":"FACET","bool_type":"MUST","facet_id":"qntfy-topic"}"""

        Mapper.unmarshal( json, classOf[ CorpexFacetQuery ] )
    }

    it should "throw an appropriate error when facet_id is invalid" in {
        val json = """{"query_type":"FACET","bool_type":"MUST","facet_id":"invalid-facet-id"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexFacetQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "invalid-facet-id is not a valid facet id" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when facet_values and facet_values_query are both defined" in {
        val json = """{"query_type":"FACET","bool_type":"MUST","facet_id":"qntfy-topic","facet_values":["some value"],"facet_values_query":"some query"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexFacetQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include ( "facet_values and facet_values_query cannot both be defined" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    behavior of "CorpexFieldAggQuery"

    it should "unmarshal json with minimal fields defined" in {
        val json = """{"agg_type":"FIELD","queried_field":"cdr.content_type"}"""

        val value = Mapper.unmarshal( json, classOf[ CorpexFieldAggQuery ] )
        value.queriedField shouldBe "cdr.content_type"
        value.bucketSize shouldBe MultiValue()
        value.aggType shouldBe AggType.FIELD
        value.hi shouldBe MultiValue()
        value.lo shouldBe MultiValue()
        value.dateHi shouldBe None
        value.dateLo shouldBe None
        value.size shouldBe None
    }

    it should "throw an appropriate error when queried_field is not INTEGER, DATE, or TERM" in {
        val json = """{"agg_type":"FIELD","queried_field":"cdr.extracted_text"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexFieldAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "cdr.extracted_text is not a field that can be aggregated" )
            case Failure( _ ) => fail( "threw exception other than InvalidSearchQueryException" )
        }
    }

    it should "throw an appropriate error when values_query is defined for a non-TERM field" in {
        val json = """{"agg_type":"FIELD","queried_field":"cdr.extracted_metadata.Pages","values_query":"test query"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexFieldAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "values_query (test query) cannot be defined for a field of type INT" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidSearchQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }
    }

    it should "throw an appropriate error when bucket_size, lo, or hi is defined for a non-DATE and non-INTEGER field" in {
        val json1 = """{"agg_type":"FIELD","queried_field":"cdr.content_type","bucket_size":1000}"""
        val json2 = """{"agg_type":"FIELD","queried_field":"cdr.content_type","lo":100}"""
        val json3 = """{"agg_type":"FIELD","queried_field":"cdr.content_type","hi":10000}"""

        Try ( Mapper.unmarshal( json1, classOf[ CorpexFieldAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "cdr.content_type does not support range query" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidSearchQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }

        Try ( Mapper.unmarshal( json2, classOf[ CorpexFieldAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "cdr.content_type does not support range query" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidSearchQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }

        Try ( Mapper.unmarshal( json3, classOf[ CorpexFieldAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidSearchQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "cdr.content_type does not support range query" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidSearchQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }
    }

    behavior of "CorpexTagTypesAggQuery"

    it should "unmarshal json with minimal fields defined" in {
        val json = """{"agg_type":"TAG_TYPES","tag_id":"qntfy-ner"}"""

        val value = Mapper.unmarshal( json, classOf[ CorpexTagTypesAggQuery ] )
        value.aggType shouldBe AggType.TAG_TYPES
        value.size shouldBe None
        value.tagId shouldBe "qntfy-ner"
        value.tagTypesQuery shouldBe None
    }

    it should "throw an appropriate error when tag_id is invalid" in {
        val json = """{"agg_type":"TAG_TYPES","tag_id":"invalid-value"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagTypesAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "invalid-value is not a valid tag id" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }
    }

    behavior of "CorpexTagValuesAggQuery"

    it should "unmarshal json with minimal fields defined" in {
        val json = """{"agg_type":"TAG_VALUES","tag_id":"qntfy-ner"}"""

        val value = Mapper.unmarshal( json, classOf[ CorpexTagValuesAggQuery ] )
        value.aggType shouldBe AggType.TAG_VALUES
        value.size shouldBe None
        value.tagId shouldBe "qntfy-ner"
        value.tagTypes shouldBe None
        value.tagTypesExact shouldBe None
        value.tagTypesQuery shouldBe None
        value.tagValuesQuery shouldBe None
    }

    it should "throw an appropriate error when tag_id is invalid" in {
        val json = """{"agg_type":"TAG_VALUES","tag_id":"invalid-value"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagValuesAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "invalid-value is not a valid tag id" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }
    }

    it should "throw an appropriate error when tag_types contains invalid tag_type" in {
        val json = """{"agg_type":"TAG_VALUES","tag_id":"qntfy-ner","tag_types":["GPE","LOC","invalid-type"]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagValuesAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "invalid-type is not a valid tag type" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }
    }

    it should "throw an appropriate error when tag_types and tag_types_exact are both defined" in {
        val json = """{"agg_type":"TAG_VALUES","tag_id":"qntfy-ner","tag_types":["GPE","LOC"],"tag_types_exact":["val1","val2"]}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexTagValuesAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "tag_types and tag_types_exact cannot both be defined" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }
    }

    behavior of "CorpexFacetAggQuery"

    it should "unmarshal json with minimal fields defined" in {
        val json = """{"agg_type":"FACET","facet_id":"qntfy-topic"}"""

        val value = Mapper.unmarshal( json, classOf[ CorpexFacetAggQuery ] )
        value.aggType shouldBe AggType.FACET
        value.size shouldBe None
        value.facetId shouldBe "qntfy-topic"
        value.facetValuesQuery shouldBe None
        value.scoreLo shouldBe None
        value.scoreLo shouldBe None
    }

    it should "throw an appropriate error when facet_id is invalid" in {
        val json = """{"agg_type":"FACET","facet_id":"invalid-id"}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexFacetAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "invalid-id is not a valid facet id" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }
    }

    it should "throw an appropriate error when score_lo or score_hi are outside of range [0,1]" in {
        val json  = """{"agg_type":"FACET","facet_id":"qntfy-topic","score_lo":-0.2}"""
        val json1 = """{"agg_type":"FACET","facet_id":"qntfy-topic","score_lo":-2}"""
        val json2 = """{"agg_type":"FACET","facet_id":"qntfy-topic","score_hi":1.2}"""
        val json3 = """{"agg_type":"FACET","facet_id":"qntfy-topic","score_hi":5}"""
        val json4 = """{"agg_type":"FACET","facet_id":"qntfy-topic","score_hi":-0.2}"""
        val json5 = """{"agg_type":"FACET","facet_id":"qntfy-topic","score_hi":-2}"""
        val json6 = """{"agg_type":"FACET","facet_id":"qntfy-topic","score_lo":1.2}"""
        val json7 = """{"agg_type":"FACET","facet_id":"qntfy-topic","score_lo":5}"""

        Try ( Mapper.unmarshal( json, classOf[ CorpexFacetAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "score_lo (-0.2) is not between 0 and 1" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }

        Try ( Mapper.unmarshal( json1, classOf[ CorpexFacetAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "score_lo (-2.0) is not between 0 and 1" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }

        Try ( Mapper.unmarshal( json2, classOf[ CorpexFacetAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "score_hi (1.2) is not between 0 and 1" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }

        Try ( Mapper.unmarshal( json3, classOf[ CorpexFacetAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "score_hi (5.0) is not between 0 and 1" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }

        Try ( Mapper.unmarshal( json4, classOf[ CorpexFacetAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "score_hi (-0.2) is not between 0 and 1" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }

        Try ( Mapper.unmarshal( json5, classOf[ CorpexFacetAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "score_hi (-2.0) is not between 0 and 1" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }

        Try ( Mapper.unmarshal( json6, classOf[ CorpexFacetAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "score_lo (1.2) is not between 0 and 1" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }

        Try ( Mapper.unmarshal( json7, classOf[ CorpexFacetAggQuery ] ) ) match {
            case Success( _ ) => fail( "did not throw InvalidAggQueryException (or any exception)" )
            case Failure( e : InvalidAggQueryException ) => e.getMessage should include ( "score_lo (5.0) is not between 0 and 1" )
            case Failure( e : Throwable ) => fail( s"threw exception other than InvalidAggQueryException: ${e.getClass}, ${e.getMessage}\n${e.getStackTrace.mkString(("\n"))}" )
        }
    }

}
