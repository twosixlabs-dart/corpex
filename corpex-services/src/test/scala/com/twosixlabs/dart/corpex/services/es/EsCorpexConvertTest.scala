package com.twosixlabs.dart.corpex.services.es

import better.files.Resource
import com.twosixlabs.cdr4s.json.dart.{ DartCdrDocumentDto, DartMetadataDto }
import com.twosixlabs.dart.corpex.api.configuration.annotations.{ EntityTagTypes, FacetIds, TagIds }
import com.twosixlabs.dart.corpex.api.enums.{ BoolType, SortType }
import com.twosixlabs.dart.corpex.api.exceptions.InvalidSearchQueryException
import com.twosixlabs.dart.corpex.api.models.{ CorpexSearchRequest, CorpexSingleResult, CorpexSortKey, Count, MultiValue, ValueCount }
import com.twosixlabs.dart.corpex.api.models.queries.{ CorpexCdrDateQuery, CorpexFacetAggQuery, CorpexFacetQuery, CorpexFieldAggQuery, CorpexIntegerQuery, CorpexTagQuery, CorpexTagValuesAggQuery, CorpexTermQuery, CorpexTextQuery }
import com.twosixlabs.dart.corpex.api.tools.Mapper
import com.twosixlabs.dart.corpex.services.search.es.models.DartPrivateFields
import com.twosixlabs.dart.corpex.services.search.es.EsCorpexConvert
import com.twosixlabs.dart.corpex.services.search.es.models.{ DartEsDocument, DartPrivateFields, DoubleRangeQuery, EsResponseHits, EsResponseHitsTotal, EsResponseResult, EsSearchResponse, LongRangeQuery, QueryStringQuery, RangeQuery }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success, Try }

class EsCorpexConvertTest extends AnyFlatSpecLike with Matchers {

    val LOG : Logger = LoggerFactory.getLogger( getClass )

    lazy val conv = new EsCorpexConvert( 10, List( "cdr.document_id", "cdr.extracted_metadata.Title" ), "cdr.extracted_text" )

    behavior of "EsCorpexConvert.corpexAggQueryToEsAggQuery"

    it should "convert a valid corpex aggregation on a term field to a terms ES aggregations query with default values" in {
        val corpexAggQuery = CorpexFieldAggQuery( queriedField = "cdr.content_type" )

        val esAggQuery = conv.corpexAggQueryToEsAggQuery( corpexAggQuery )
        esAggQuery.aggs shouldBe None
        esAggQuery.date shouldBe None
        esAggQuery.filter shouldBe None
        esAggQuery.histogram shouldBe None
        esAggQuery.nested shouldBe None
        val termAggQuery = esAggQuery.terms.get
        termAggQuery.field shouldBe Some( "content_type.term" )
        termAggQuery.size shouldBe Some( 200 )
        termAggQuery.calendarInt shouldBe None
        termAggQuery.fixedInt shouldBe None
        termAggQuery.path shouldBe None
        termAggQuery.format shouldBe None
    }

    it should "convert a valid corpex aggregation on a date field to a date_histogram ES aggregations query with default values" in {
        val corpexAggQuery = CorpexFieldAggQuery( queriedField = "cdr.timestamp" )

        val esAggQuery = conv.corpexAggQueryToEsAggQuery( corpexAggQuery )
        esAggQuery.aggs shouldBe None
        esAggQuery.terms shouldBe None
        esAggQuery.filter shouldBe None
        esAggQuery.histogram shouldBe None
        esAggQuery.nested shouldBe None
        val termAggQuery = esAggQuery.date.get
        termAggQuery.field shouldBe Some( "timestamp" )
        termAggQuery.size shouldBe None
        termAggQuery.calendarInt shouldBe Some( "1M" )
        termAggQuery.fixedInt shouldBe None
        termAggQuery.path shouldBe None
        termAggQuery.format shouldBe Some( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" )
    }

    it should "convert a valid corpex tag aggregation to a nested ES aggregations query with default values" in {
        val corpexAggQuery = CorpexTagValuesAggQuery( tagId = "qntfy-ner", tagTypes = Some( List( "GPE" ) ) )

        val esAggQuery = conv.corpexAggQueryToEsAggQuery( corpexAggQuery, "tag-query" )

        esAggQuery.terms shouldBe None
        esAggQuery.filter shouldBe None
        esAggQuery.histogram shouldBe None
        esAggQuery.date shouldBe None
        val nestedAggQuery = esAggQuery.nested.get
        nestedAggQuery.field shouldBe None
        nestedAggQuery.size shouldBe None
        nestedAggQuery.calendarInt shouldBe None
        nestedAggQuery.fixedInt shouldBe None
        nestedAggQuery.path shouldBe Some( "annotations" )
        nestedAggQuery.format shouldBe None
        val nestedAggAgg = esAggQuery.aggs.get
        nestedAggAgg.size shouldBe 1
        val labelFilter = nestedAggAgg( "tag-query_label_filter" )
        labelFilter.nested shouldBe None
        labelFilter.histogram shouldBe None
        labelFilter.date shouldBe None
        labelFilter.reverseNested shouldBe None
        labelFilter.terms shouldBe None
        val labelFilterFilter = labelFilter.filter.get
        labelFilterFilter.queryStringQuery shouldBe None
        labelFilterFilter.nestedQuery shouldBe None
        labelFilterFilter.boolQuery shouldBe None
        labelFilterFilter.rangeQuery shouldBe None
        labelFilterFilter.matchQuery shouldBe None
        val labelFilterTermQuery = labelFilterFilter.termQuery.get
        labelFilterTermQuery.size shouldBe 1
        labelFilterTermQuery( "annotations.label.term" ).boost shouldBe None
        labelFilterTermQuery( "annotations.label.term" ).value shouldBe "qntfy-ner-annotator"
        val labelFilterAggs = labelFilter.aggs.get
        labelFilterAggs.size shouldBe 1
        val content = labelFilterAggs( "tag-query_content" )
        content.nested.get.path shouldBe Some( "annotations.content" )
        val tagFilter = content.aggs.get( "tag-query_tag_filter" )
        tagFilter.filter.get.boolQuery.get.should.get.size shouldBe 1
        tagFilter.filter.get.boolQuery.get.should.get.head.termQuery.get( "annotations.content.tag.term" ).value shouldBe "GPE"
        tagFilter.aggs.get( "tag-query_terms" ).terms.get.field shouldBe Some( "annotations.content.value.term" )
        tagFilter.aggs.get( "tag-query_terms" ).terms.get.size shouldBe Some( 200 )
        tagFilter.aggs.get( "tag-query_terms" ).aggs.get( "tag-query_docs" ).reverseNested.isDefined shouldBe true
        tagFilter.aggs.get( "tag-query_terms" ).aggs.get( "tag-query_docs" ).reverseNested.get.path shouldBe None
    }

    it should "convert a valid corpex tag aggregation to a nested ES aggregations query with a query string filter" in {
        val corpexAggQuery = CorpexTagValuesAggQuery( tagId = "qntfy-ner", tagTypes = Some( List( "GPE" ) ), tagValuesQuery = Some( "test value" ) )

        val esAggQuery = conv.corpexAggQueryToEsAggQuery( corpexAggQuery, "tag-query" )

        esAggQuery.terms shouldBe None
        esAggQuery.filter shouldBe None
        esAggQuery.histogram shouldBe None
        esAggQuery.date shouldBe None
        val nestedAggQuery = esAggQuery.nested.get
        nestedAggQuery.field shouldBe None
        nestedAggQuery.size shouldBe None
        nestedAggQuery.calendarInt shouldBe None
        nestedAggQuery.fixedInt shouldBe None
        nestedAggQuery.path shouldBe Some( "annotations" )
        nestedAggQuery.format shouldBe None
        val nestedAggAgg = esAggQuery.aggs.get
        nestedAggAgg.size shouldBe 1
        val labelFilter = nestedAggAgg( "tag-query_label_filter" )
        labelFilter.nested shouldBe None
        labelFilter.histogram shouldBe None
        labelFilter.date shouldBe None
        labelFilter.reverseNested shouldBe None
        labelFilter.terms shouldBe None
        val labelFilterFilter = labelFilter.filter.get
        labelFilterFilter.queryStringQuery shouldBe None
        labelFilterFilter.nestedQuery shouldBe None
        labelFilterFilter.boolQuery shouldBe None
        labelFilterFilter.rangeQuery shouldBe None
        labelFilterFilter.matchQuery shouldBe None
        val labelFilterTermQuery = labelFilterFilter.termQuery.get
        labelFilterTermQuery.size shouldBe 1
        labelFilterTermQuery( "annotations.label.term" ).boost shouldBe None
        labelFilterTermQuery( "annotations.label.term" ).value shouldBe "qntfy-ner-annotator"
        val labelFilterAggs = labelFilter.aggs.get
        labelFilterAggs.size shouldBe 1
        val content = labelFilterAggs( "tag-query_content" )
        content.nested.get.path shouldBe Some( "annotations.content" )
        val tagFilter = content.aggs.get( "tag-query_tag_filter" )
        val tagTagFilterBool = tagFilter.filter.get.boolQuery.get
        tagTagFilterBool.filter.get.size shouldBe 2
        val tagTagFilterBoolTypes = tagTagFilterBool.filter.get.head.boolQuery.get.should.get
        tagTagFilterBoolTypes.size shouldBe 1
        tagTagFilterBoolTypes.head.termQuery.get( "annotations.content.tag.term" ).boost shouldBe None
        tagTagFilterBoolTypes.head.termQuery.get( "annotations.content.tag.term" ).value shouldBe "GPE"
        val tagTagFilterBoolValues = tagTagFilterBool.filter.get(1).queryStringQuery.get
        tagTagFilterBoolValues.fields shouldBe List( "annotations.content.value" )
        tagTagFilterBoolValues.query shouldBe "test value"
        tagFilter.aggs.get( "tag-query_terms" ).terms.get.field shouldBe Some( "annotations.content.value.term" )
        tagFilter.aggs.get( "tag-query_terms" ).terms.get.size shouldBe Some( 200 )
        tagFilter.aggs.get( "tag-query_terms" ).aggs.get( "tag-query_docs" ).reverseNested.isDefined shouldBe true
        tagFilter.aggs.get( "tag-query_terms" ).aggs.get( "tag-query_docs" ).reverseNested.get.path shouldBe None
    }

    it should "convert a valid corpex facet aggregation to a nested ES aggregations query with default values and no score filter" in {
        val corpexAggQuery = CorpexFacetAggQuery( facetId = "factiva-subject" )

        val esAggQuery = conv.corpexAggQueryToEsAggQuery( corpexAggQuery, "facet-query" )
        esAggQuery.terms shouldBe None
        esAggQuery.filter shouldBe None
        esAggQuery.histogram shouldBe None
        esAggQuery.date shouldBe None
        val nestedAggQuery = esAggQuery.nested.get
        nestedAggQuery.field shouldBe None
        nestedAggQuery.size shouldBe None
        nestedAggQuery.calendarInt shouldBe None
        nestedAggQuery.fixedInt shouldBe None
        nestedAggQuery.path shouldBe Some( "annotations" )
        nestedAggQuery.format shouldBe None
        val nestedAggAgg = esAggQuery.aggs.get
        nestedAggAgg.size shouldBe 1
        val labelFilter = nestedAggAgg( "facet-query_label_filter" )
        labelFilter.nested shouldBe None
        labelFilter.histogram shouldBe None
        labelFilter.date shouldBe None
        labelFilter.reverseNested shouldBe None
        labelFilter.terms shouldBe None
        val labelFilterFilter = labelFilter.filter.get
        labelFilterFilter.queryStringQuery shouldBe None
        labelFilterFilter.nestedQuery shouldBe None
        labelFilterFilter.boolQuery shouldBe None
        labelFilterFilter.rangeQuery shouldBe None
        labelFilterFilter.matchQuery shouldBe None
        val labelFilterTermQuery = labelFilterFilter.termQuery.get
        labelFilterTermQuery.size shouldBe 1
        labelFilterTermQuery( "annotations.label.term" ).boost shouldBe None
        labelFilterTermQuery( "annotations.label.term" ).value shouldBe "factiva-subjects"
        val labelFilterAggs = labelFilter.aggs.get
        labelFilterAggs.size shouldBe 1
        val content = labelFilterAggs( "facet-query_content" )
        content.nested.get.path shouldBe Some( "annotations.content" )
        content.aggs.get( "facet-query_terms" ).terms.get.field shouldBe Some( "annotations.content.value.term" )
        content.aggs.get( "facet-query_terms" ).terms.get.size shouldBe Some( 200 )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_docs" ).reverseNested.isDefined shouldBe true
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_docs" ).reverseNested.get.path shouldBe None
    }

    it should "convert a valid corpex facet aggregation to a nested ES aggregations query with a score filter" in {
        val corpexAggQuery = CorpexFacetAggQuery( facetId = "factiva-subject", scoreLo = Some( 0.25 ), scoreHi = Some( 0.75 ) )

        val esAggQuery = conv.corpexAggQueryToEsAggQuery( corpexAggQuery, "facet-query" )
        esAggQuery.terms shouldBe None
        esAggQuery.filter shouldBe None
        esAggQuery.histogram shouldBe None
        esAggQuery.date shouldBe None
        val nestedAggQuery = esAggQuery.nested.get
        nestedAggQuery.field shouldBe None
        nestedAggQuery.size shouldBe None
        nestedAggQuery.calendarInt shouldBe None
        nestedAggQuery.fixedInt shouldBe None
        nestedAggQuery.path shouldBe Some( "annotations" )
        nestedAggQuery.format shouldBe None
        val nestedAggAgg = esAggQuery.aggs.get
        nestedAggAgg.size shouldBe 1
        val labelFilter = nestedAggAgg( "facet-query_label_filter" )
        labelFilter.nested shouldBe None
        labelFilter.histogram shouldBe None
        labelFilter.date shouldBe None
        labelFilter.reverseNested shouldBe None
        labelFilter.terms shouldBe None
        val labelFilterFilter = labelFilter.filter.get
        labelFilterFilter.queryStringQuery shouldBe None
        labelFilterFilter.nestedQuery shouldBe None
        labelFilterFilter.boolQuery shouldBe None
        labelFilterFilter.rangeQuery shouldBe None
        labelFilterFilter.matchQuery shouldBe None
        val labelFilterTermQuery = labelFilterFilter.termQuery.get
        labelFilterTermQuery.size shouldBe 1
        labelFilterTermQuery( "annotations.label.term" ).boost shouldBe None
        labelFilterTermQuery( "annotations.label.term" ).value shouldBe "factiva-subjects"
        val labelFilterAggs = labelFilter.aggs.get
        labelFilterAggs.size shouldBe 1
        val content = labelFilterAggs( "facet-query_content" )
        content.nested.get.path shouldBe Some( "annotations.content" )
        content.aggs.get.keySet should contain( "facet-query_terms" )
        content.aggs.get( "facet-query_terms" ).aggs.get.keySet should contain( "facet-query_facet_filter" )
        val range = content.aggs.get( "facet-query_terms" ).filter.get.rangeQuery.get( "annotations.content.score" ).asInstanceOf[ DoubleRangeQuery ]
        range.format shouldBe None
        range.gte shouldBe Some( 0.25 )
        range.lte shouldBe Some( 0.75 )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).terms.get.field shouldBe Some( "annotations.content.value.term" )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).terms.get.size shouldBe Some( 200 )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).aggs.get( "facet-query_docs" ).reverseNested.isDefined shouldBe true
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).aggs.get( "facet-query_docs" ).reverseNested.get.path shouldBe None
    }

    it should "convert a valid corpex facet aggregation to a nested ES aggregations query with a query string filter" in {
        val corpexAggQuery = CorpexFacetAggQuery( facetId = "factiva-subject", facetValuesQuery = Some( "test value" ) )

        val esAggQuery = conv.corpexAggQueryToEsAggQuery( corpexAggQuery, "facet-query" )
        val nestedAggAgg = esAggQuery.aggs.get
        nestedAggAgg.size shouldBe 1
        val labelFilter = nestedAggAgg( "facet-query_label_filter" )
        val labelFilterFilter = labelFilter.filter.get
        val labelFilterTermQuery = labelFilterFilter.termQuery.get
        labelFilterTermQuery.size shouldBe 1
        labelFilterTermQuery( "annotations.label.term" ).boost shouldBe None
        labelFilterTermQuery( "annotations.label.term" ).value shouldBe "factiva-subjects"
        val labelFilterAggs = labelFilter.aggs.get
        labelFilterAggs.size shouldBe 1
        val content = labelFilterAggs( "facet-query_content" )
        content.nested.get.path shouldBe Some( "annotations.content" )
        val qsFilter : QueryStringQuery = content.aggs.get( "facet-query_terms" ).filter.get.queryStringQuery.get
        qsFilter.query shouldBe "test value"
        qsFilter.fields shouldBe List( "annotations.content.value" )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).terms.get.field shouldBe Some( "annotations.content.value.term" )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).terms.get.size shouldBe Some( 200 )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).aggs.get( "facet-query_docs" ).reverseNested.isDefined shouldBe true
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).aggs.get( "facet-query_docs" ).reverseNested.get.path shouldBe None
    }

    it should "convert a valid corpex facet aggregation to a nested ES aggregations query with a query string filter AND a score filter" in {
        val corpexAggQuery = CorpexFacetAggQuery( facetId = "factiva-subject", facetValuesQuery = Some( "test value" ), scoreLo = Some( 0.25 ), scoreHi = Some( 0.75 ) )

        val esAggQuery = conv.corpexAggQueryToEsAggQuery( corpexAggQuery, "facet-query" )
        val nestedAggAgg = esAggQuery.aggs.get
        nestedAggAgg.size shouldBe 1
        val labelFilter = nestedAggAgg( "facet-query_label_filter" )
        val labelFilterFilter = labelFilter.filter.get
        val labelFilterTermQuery = labelFilterFilter.termQuery.get
        labelFilterTermQuery.size shouldBe 1
        labelFilterTermQuery( "annotations.label.term" ).boost shouldBe None
        labelFilterTermQuery( "annotations.label.term" ).value shouldBe "factiva-subjects"
        val labelFilterAggs = labelFilter.aggs.get
        labelFilterAggs.size shouldBe 1
        val content = labelFilterAggs( "facet-query_content" )
        content.nested.get.path shouldBe Some( "annotations.content" )
        val qsFilter : QueryStringQuery = content.aggs.get( "facet-query_terms" ).filter.get.boolQuery.get.filter.get.head.queryStringQuery.get
        qsFilter.query shouldBe "test value"
        qsFilter.fields shouldBe List( "annotations.content.value" )
        val rangeFilter = content.aggs.get( "facet-query_terms" ).filter.get.boolQuery.get.filter.get.last.rangeQuery.get( "annotations.content.score" ).asInstanceOf[
          DoubleRangeQuery ]
        rangeFilter.format shouldBe None
        rangeFilter.gte shouldBe Some( 0.25 )
        rangeFilter.lte shouldBe Some( 0.75 )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).terms.get.field shouldBe Some( "annotations.content.value.term" )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).terms.get.size shouldBe Some( 200 )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).aggs.get( "facet-query_docs" ).reverseNested.isDefined shouldBe true
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).aggs.get( "facet-query_docs" ).reverseNested.get.path shouldBe None
    }


    behavior of "EsCorpexConvert.corpexQueryToEsQuery"

    it should "convert a valid integer query to a valid ES query" in {
        val corpexQuery = CorpexIntegerQuery( boolType = BoolType.MUST,
                                              queriedFields = List( "cdr.extracted_metadata.Pages" ),
                                              intLo = Some( 5 ),
                                              intHi = Some( 10 ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.rangeQuery.isDefined shouldBe true
        esQueryConverted.queryStringQuery.isDefined shouldBe false
        esQueryConverted.boolQuery.isDefined shouldBe false
        esQueryConverted.matchQuery.isDefined shouldBe false
        esQueryConverted.termQuery.isDefined shouldBe false

        val rangeQuery : Map[ String, RangeQuery ] = esQueryConverted.rangeQuery.get
        rangeQuery( "extracted_metadata.Pages" ).asInstanceOf[ LongRangeQuery ].gte.get shouldBe 5
        rangeQuery( "extracted_metadata.Pages" ).asInstanceOf[ LongRangeQuery ].lte.get shouldBe 10
        rangeQuery( "extracted_metadata.Pages" ).asInstanceOf[ LongRangeQuery ].format shouldBe None
    }

    it should "throw a  exception if any queried fields are not of integer data type" in {
        Try {
            val corpexQuery = CorpexIntegerQuery( boolType = BoolType.MUST,
                                                  queriedFields = List( "cdr.extracted_metadata.Pages", "cdr.content_type", "word_count" ),
                                                  intLo = Some( 5 ),
                                                  intHi = Some( 10 ) )

            conv.corpexQueryToEsQuery( corpexQuery )
        } match {
            case Success( _ ) => fail
            case Failure( e : InvalidSearchQueryException ) => e.getMessage should include( "cdr.content_type" )
            case Failure( _ ) => fail
        }
    }

    it should "convert a valid term query to a valid ES query" in {
        val corpexQuery = CorpexTermQuery( boolType = BoolType.MUST,
                                           queriedField = "cdr.content_type",
                                           termValues = List( "application/json" ),
                                           valuesBoolType = Some( BoolType.MUST_NOT ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.boolQuery.get.mustNot.get.head.termQuery.get( "content_type.term" ).value shouldBe "application/json"
    }

    it should "convert a valid term query with multiple values to a valid ES bool query" in {
        val corpexQuery = CorpexTermQuery( boolType = BoolType.MUST,
                                           queriedField = "cdr.content_type",
                                           termValues = List( "application/json", "Other Content Type" ),
                                           valuesBoolType = Some( BoolType.SHOULD ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.boolQuery.get.should.get.head.termQuery.get( "content_type.term" ).value shouldBe "application/json"
        esQueryConverted.boolQuery.get.should.get( 1 ).termQuery.get( "content_type.term" ).value shouldBe "Other Content Type"
    }

    it should "convert a valid term query string query into an ES query" in {
        val corpexQuery = CorpexTermQuery( boolType = BoolType.MUST,
                                           queriedField = "cdr.content_type",
                                           termValues = List( "application/json" ),
                                           valuesBoolType = Some( BoolType.SHOULD ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.boolQuery.get.should.get.head.termQuery.get("content_type.term").value shouldBe "application/json"
    }

    it should "convert a valid term query string query with multiple term values into an ES query" in {
        val corpexQuery = CorpexTermQuery( boolType = BoolType.MUST,
                                           queriedField = "cdr.content_type",
                                           termValues = List( "application/json", "Another Type" ),
                                           valuesBoolType = Some( BoolType.SHOULD ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.boolQuery.get.should.get.head.termQuery.get("content_type.term").value shouldBe "application/json"
        esQueryConverted.boolQuery.get.should.get( 1 ).termQuery.get("content_type.term").value shouldBe "Another Type"
    }

    it should "convert a valid full text query to a valid ES query" in {
        val corpexQuery = CorpexTextQuery( boolType = BoolType.MUST,
                                           queryString = "ethiopia +gambella",
                                           queriedFields = List( "cdr.extracted_text" ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.queryStringQuery.isDefined shouldBe true
        esQueryConverted.rangeQuery.isDefined shouldBe false
        esQueryConverted.boolQuery.isDefined shouldBe false
        esQueryConverted.matchQuery.isDefined shouldBe false
        esQueryConverted.termQuery.isDefined shouldBe false

        val queryStringQuery = esQueryConverted.queryStringQuery.get
        queryStringQuery.fields should contain( "extracted_text" )
        queryStringQuery.query shouldBe "ethiopia +gambella"
    }

    it should "return throw an appropriate error when given an invalid Corpex text query" in {
        an[ InvalidSearchQueryException ] should be thrownBy CorpexTextQuery( boolType = BoolType.MUST,
                                                                              queriedFields = List( "cdr.extracted_metadata.CreationDate" ),
                                                                              queryString = "sdfasd" )
    }

    it should "convert a valid cdr date query to a valid ES query" in {
        val corpexQuery = CorpexCdrDateQuery( boolType = BoolType.MUST,
                                              queriedFields = List( "cdr.extracted_metadata.CreationDate" ),
                                              dateLo = MultiValue( long = Some( 1490745600000L ) ),
                                              dateHi = MultiValue( long = Some( 1490792399000L ) ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.rangeQuery.isDefined shouldBe true
        esQueryConverted.queryStringQuery.isDefined shouldBe false
        esQueryConverted.boolQuery.isDefined shouldBe false
        esQueryConverted.matchQuery.isDefined shouldBe false
        esQueryConverted.termQuery.isDefined shouldBe false

        val rangeQuery : Map[ String, RangeQuery ] = esQueryConverted.rangeQuery.get
        rangeQuery( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].gte.get shouldBe 1490745600000L
        rangeQuery( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].lte.get shouldBe 1490792399000L
        rangeQuery( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].format.get shouldBe "epoch_millis"
    }

    it should "throw an exception when given an invalid cdr date query (missing range)" in {
        an[ InvalidSearchQueryException ] should be thrownBy CorpexCdrDateQuery( boolType = BoolType.MUST,
                                                                                 queriedFields = List( "cdr.extracted_metadata.CreationDate" ) )
    }

    it should "convert a valid basic multi-entity search query to a valid ES query" in {
        val corpexQuery = CorpexTagQuery( boolType = BoolType.MUST,
                                          tagId = TagIds.QNTFY_NER.apiLabel,
                                          tagTypes = Some( List( EntityTagTypes.ORG.apiLabel ) ),
                                          tagValues = Some( List( "Org 1", "org 2", "organization 3" ) ),
                                          valuesBoolType = Some( BoolType.FILTER ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.nestedQuery.isDefined shouldBe true
        esQueryConverted.queryStringQuery.isDefined shouldBe false
        esQueryConverted.rangeQuery.isDefined shouldBe false
        esQueryConverted.matchQuery.isDefined shouldBe false
        esQueryConverted.termQuery.isDefined shouldBe false

        val nestedQuery = esQueryConverted.nestedQuery.get
        nestedQuery.path shouldBe "annotations"
        val boolQuery = nestedQuery.query.boolQuery.get
        boolQuery.must.get.head.termQuery.get( "annotations.label.term" ).value shouldBe "qntfy-ner-annotator"
        val subBoolQuery = boolQuery.must.get( 1 ).boolQuery.get
        val valuesQueries = subBoolQuery.filter.get
        valuesQueries.size shouldBe 3
        valuesQueries.head.nestedQuery.get.path shouldBe "annotations.content"
        valuesQueries.head.nestedQuery.get.query.boolQuery.get.must.get.size shouldBe 2
        valuesQueries.head.nestedQuery.get.query.boolQuery.get.must.get.head.termQuery.get( "annotations.content.tag.term" ).value shouldBe "ORG"
        valuesQueries.head.nestedQuery.get.query.boolQuery.get.must.get( 1 ).termQuery.get( "annotations.content.value.term" ).value shouldBe "Org 1"
        valuesQueries( 1 ).nestedQuery.get.path shouldBe "annotations.content"
        valuesQueries( 1 ).nestedQuery.get.query.boolQuery.get.must.get.size shouldBe 2
        valuesQueries( 1 ).nestedQuery.get.query.boolQuery.get.must.get.head.termQuery.get( "annotations.content.tag.term" ).value shouldBe "ORG"
        valuesQueries( 1 ).nestedQuery.get.query.boolQuery.get.must.get( 1 ).termQuery.get( "annotations.content.value.term" ).value shouldBe "org 2"
        valuesQueries( 2 ).nestedQuery.get.path shouldBe "annotations.content"
        valuesQueries( 2 ).nestedQuery.get.query.boolQuery.get.must.get.size shouldBe 2
        valuesQueries( 2 ).nestedQuery.get.query.boolQuery.get.must.get.head.termQuery.get( "annotations.content.tag.term" ).value shouldBe "ORG"
        valuesQueries( 2 ).nestedQuery.get.query.boolQuery.get.must.get(1).termQuery.get( "annotations.content.value.term" ).value shouldBe "organization 3"
    }

    it should "convert a valid entity-query-string search query to a valid ES query" in {
        val corpexQuery = CorpexTagQuery( boolType = BoolType.MUST,
                                          tagId = TagIds.QNTFY_NER.apiLabel,
                                          tagTypes = Some( List( EntityTagTypes.ORG.apiLabel ) ),
                                          tagValuesQuery = Some( "Some organization query" ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.nestedQuery.isDefined shouldBe true
        esQueryConverted.queryStringQuery.isDefined shouldBe false
        esQueryConverted.rangeQuery.isDefined shouldBe false
        esQueryConverted.matchQuery.isDefined shouldBe false
        esQueryConverted.termQuery.isDefined shouldBe false

        val nestedQuery = esQueryConverted.nestedQuery.get
        nestedQuery.path shouldBe "annotations"
        val boolQuery = nestedQuery.query.boolQuery.get
        boolQuery.must.get.head.termQuery.get( "annotations.label.term" ).value shouldBe "qntfy-ner-annotator"
        val subNestedQuery = boolQuery.must.get( 1 ).nestedQuery.get
        subNestedQuery.path shouldBe "annotations.content"
        subNestedQuery.query.boolQuery.get.must.get.head.termQuery.get( "annotations.content.tag.term" ).value shouldBe "ORG"
        subNestedQuery.query.boolQuery.get.must.get( 1 ).queryStringQuery.get.fields shouldBe List( "annotations.content.value" )
        subNestedQuery.query.boolQuery.get.must.get( 1 ).queryStringQuery.get.query shouldBe "Some organization query"
        subNestedQuery.query.boolQuery.get.must.get.length shouldBe 2
    }

    it should "convert a valid facet query string search query to a valid EsQuery" in {
        val corpexQuery = CorpexFacetQuery( boolType = BoolType.MUST,
                                            facetId = FacetIds.QNTFY_TOPIC.apiLabel,
                                            facetValuesQuery = Some( "Some organization query" ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.nestedQuery.isDefined shouldBe true
        esQueryConverted.queryStringQuery.isDefined shouldBe false
        esQueryConverted.rangeQuery.isDefined shouldBe false
        esQueryConverted.matchQuery.isDefined shouldBe false
        esQueryConverted.termQuery.isDefined shouldBe false

        val nestedQuery = esQueryConverted.nestedQuery.get
        nestedQuery.path shouldBe "annotations"
        val boolQuery = nestedQuery.query.boolQuery.get
        boolQuery.must.get.head.termQuery.get( "annotations.label.term" ).value shouldBe "qntfy-categories-annotator"
        val subNestedQuery = boolQuery.must.get( 1 ).nestedQuery.get
        subNestedQuery.path shouldBe "annotations.content"
        subNestedQuery.query.queryStringQuery.get.fields shouldBe List( "annotations.content.value" )
        subNestedQuery.query.queryStringQuery.get.query shouldBe "Some organization query"
    }

    it should "convert a valid facet values search query to a valid EsQuery" in {
        val corpexQuery = CorpexFacetQuery( boolType = BoolType.MUST,
                                            facetId = FacetIds.QNTFY_TOPIC.apiLabel,
                                            facetValues = Some( List( "Org 1", "org 2", "organization 3" ) ),
                                            valuesBoolType = Some( BoolType.MUST_NOT ))

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.nestedQuery.isDefined shouldBe true
        esQueryConverted.queryStringQuery.isDefined shouldBe false
        esQueryConverted.rangeQuery.isDefined shouldBe false
        esQueryConverted.matchQuery.isDefined shouldBe false
        esQueryConverted.termQuery.isDefined shouldBe false

        val nestedQuery = esQueryConverted.nestedQuery.get
        nestedQuery.path shouldBe "annotations"
        val boolQuery = nestedQuery.query.boolQuery.get
        boolQuery.must.get.head.termQuery.get( "annotations.label.term" ).value shouldBe "qntfy-categories-annotator"
        val subBoolQuery = boolQuery.must.get( 1 ).boolQuery.get
        subBoolQuery.mustNot.get.length shouldBe 3
        subBoolQuery.mustNot.get.head.nestedQuery.get.path shouldBe "annotations.content"
        subBoolQuery.mustNot.get.head.nestedQuery.get.query.termQuery.get( "annotations.content.value.term" ).value shouldBe "Org 1"
        subBoolQuery.mustNot.get( 1 ).nestedQuery.get.path shouldBe "annotations.content"
        subBoolQuery.mustNot.get( 1 ).nestedQuery.get.query.termQuery.get( "annotations.content.value.term" ).value shouldBe "org 2"
        subBoolQuery.mustNot.get( 2 ).nestedQuery.get.path shouldBe "annotations.content"
        subBoolQuery.mustNot.get( 2 ).nestedQuery.get.query.termQuery.get( "annotations.content.value.term" ).value shouldBe "organization 3"
    }

    it should "convert a facet search query with values AND score bounds to a valid EsQuery" in {
        val corpexQuery = CorpexFacetQuery( boolType = BoolType.MUST,
                                            facetId = FacetIds.QNTFY_TOPIC.apiLabel,
                                            facetValues = Some( List( "Org 1", "org 2", "organization 3" ) ),
                                            valuesBoolType = Some( BoolType.MUST_NOT ),
                                            scoreHi = Some( 0.789 ),
                                            scoreLo = Some( 0.4975 ) )

        val esQueryConverted = conv.corpexQueryToEsQuery( corpexQuery )

        esQueryConverted.nestedQuery.isDefined shouldBe true
        esQueryConverted.queryStringQuery.isDefined shouldBe false
        esQueryConverted.rangeQuery.isDefined shouldBe false
        esQueryConverted.matchQuery.isDefined shouldBe false
        esQueryConverted.termQuery.isDefined shouldBe false

        val nestedQuery = esQueryConverted.nestedQuery.get
        nestedQuery.path shouldBe "annotations"
        val boolQuery = nestedQuery.query.boolQuery.get
        boolQuery.must.get.head.termQuery.get( "annotations.label.term" ).value shouldBe "qntfy-categories-annotator"
        val subBoolQuery = boolQuery.must.get( 1 ).boolQuery.get
        subBoolQuery.mustNot.get.size shouldBe 3
        val queryQuery1 = subBoolQuery.mustNot.get.head.nestedQuery.get
        queryQuery1.path shouldBe "annotations.content"
        queryQuery1.query.boolQuery.get.must.get.head.termQuery.get( "annotations.content.value.term" ).value shouldBe "Org 1"
        queryQuery1.query.boolQuery.get.filter.get.head.rangeQuery.get( "annotations.content.score" ).asInstanceOf[ DoubleRangeQuery ].gte.get shouldBe 0.4975
        queryQuery1.query.boolQuery.get.filter.get.head.rangeQuery.get( "annotations.content.score" ).asInstanceOf[ DoubleRangeQuery ].lte.get shouldBe 0.789
        val queryQuery2 = subBoolQuery.mustNot.get( 1 ).nestedQuery.get
        queryQuery2.path shouldBe "annotations.content"
        queryQuery2.query.boolQuery.get.must.get.head.termQuery.get( "annotations.content.value.term" ).value shouldBe "org 2"
        queryQuery2.query.boolQuery.get.filter.get.head.rangeQuery.get( "annotations.content.score" ).asInstanceOf[ DoubleRangeQuery ].gte.get shouldBe 0.4975
        queryQuery2.query.boolQuery.get.filter.get.head.rangeQuery.get( "annotations.content.score" ).asInstanceOf[ DoubleRangeQuery ].lte.get shouldBe 0.789
        val queryQuery3 = subBoolQuery.mustNot.get( 2 ).nestedQuery.get
        queryQuery3.path shouldBe "annotations.content"
        queryQuery3.query.boolQuery.get.must.get.head.termQuery.get( "annotations.content.value.term" ).value shouldBe "organization 3"
        queryQuery3.query.boolQuery.get.filter.get.head.rangeQuery.get( "annotations.content.score" ).asInstanceOf[ DoubleRangeQuery ].gte.get shouldBe 0.4975
        queryQuery3.query.boolQuery.get.filter.get.head.rangeQuery.get( "annotations.content.score" ).asInstanceOf[ DoubleRangeQuery ].lte.get shouldBe 0.789
    }

    behavior of "EsCorpexConvert.corpexRequestToEsRequest"

    it should "convert a CorpexSearchRequest with aggs defined" in {

        val termQuery = CorpexFieldAggQuery( queriedField = "cdr.content_type" )
        val facetQuery = CorpexFacetAggQuery( facetId = "factiva-subject", scoreLo = Some( 0.25 ), scoreHi = Some( 0.75 ) )

        val corpexRequest = CorpexSearchRequest( page = Some( 1 ),
                                                 pageSize = Some( 10 ),
                                                 queries = Some( Nil ),
                                                 aggs = Some( Map( "facet-query" -> facetQuery, "term-query" -> termQuery ) ) )

        val esRequest = conv.corpexRequestToEsRequest( corpexRequest )

        esRequest.aggs.get.size shouldBe 2

        val esAggQuery = esRequest.aggs.get( "term-query" )
        esAggQuery.aggs shouldBe None
        esAggQuery.date shouldBe None
        esAggQuery.filter shouldBe None
        esAggQuery.histogram shouldBe None
        esAggQuery.nested shouldBe None
        val termAggQuery = esAggQuery.terms.get
        termAggQuery.field shouldBe Some( "content_type.term" )
        termAggQuery.size shouldBe Some( 200 )
        termAggQuery.calendarInt shouldBe None
        termAggQuery.fixedInt shouldBe None
        termAggQuery.path shouldBe None
        termAggQuery.format shouldBe None

        val esFacetAggQuery = esRequest.aggs.get( "facet-query" )
        esFacetAggQuery.terms shouldBe None
        esFacetAggQuery.filter shouldBe None
        esFacetAggQuery.histogram shouldBe None
        esFacetAggQuery.date shouldBe None
        val nestedAggQuery = esFacetAggQuery.nested.get
        nestedAggQuery.field shouldBe None
        nestedAggQuery.size shouldBe None
        nestedAggQuery.calendarInt shouldBe None
        nestedAggQuery.fixedInt shouldBe None
        nestedAggQuery.path shouldBe Some( "annotations" )
        nestedAggQuery.format shouldBe None
        val nestedAggAgg = esFacetAggQuery.aggs.get
        nestedAggAgg.size shouldBe 1
        val labelFilter = nestedAggAgg( "facet-query_label_filter" )
        labelFilter.nested shouldBe None
        labelFilter.histogram shouldBe None
        labelFilter.date shouldBe None
        labelFilter.reverseNested shouldBe None
        labelFilter.terms shouldBe None
        val labelFilterFilter = labelFilter.filter.get
        labelFilterFilter.queryStringQuery shouldBe None
        labelFilterFilter.nestedQuery shouldBe None
        labelFilterFilter.boolQuery shouldBe None
        labelFilterFilter.rangeQuery shouldBe None
        labelFilterFilter.matchQuery shouldBe None
        val labelFilterTermQuery = labelFilterFilter.termQuery.get
        labelFilterTermQuery.size shouldBe 1
        labelFilterTermQuery( "annotations.label.term" ).boost shouldBe None
        labelFilterTermQuery( "annotations.label.term" ).value shouldBe "factiva-subjects"
        val labelFilterAggs = labelFilter.aggs.get
        labelFilterAggs.size shouldBe 1
        val content = labelFilterAggs( "facet-query_content" )
        content.nested.get.path shouldBe Some( "annotations.content" )
        content.aggs.get.keySet should contain( "facet-query_terms" )
        content.aggs.get( "facet-query_terms" ).aggs.get.keySet should contain( "facet-query_facet_filter" )
        val range = content.aggs.get( "facet-query_terms" ).filter.get.rangeQuery.get( "annotations.content.score" ).asInstanceOf[ DoubleRangeQuery ]
        range.format shouldBe None
        range.gte shouldBe Some( 0.25 )
        range.lte shouldBe Some( 0.75 )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).terms.get.field shouldBe Some( "annotations.content.value.term" )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).terms.get.size shouldBe Some( 200 )
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).aggs.get( "facet-query_docs" ).reverseNested.isDefined shouldBe true
        content.aggs.get( "facet-query_terms" ).aggs.get( "facet-query_facet_filter" ).aggs.get( "facet-query_docs" ).reverseNested.get.path shouldBe None
    }

    it should "convert a CorpexSearchRequest with no fields values to an ES request with doc id and title fields" in {
        val corpexRequest = CorpexSearchRequest( page = Some( 1 ),
                                                 pageSize = Some( 10 ),
                                                 queries = Some( List() ) )

        val esRequest = conv.corpexRequestToEsRequest( corpexRequest )

        val fields : List[ String ] = esRequest.source.get
        fields.length shouldBe 2
        fields should contain( "document_id" )
        fields should contain( "extracted_metadata.Title" )
    }

    it should "convert a CorpexSearchRequest with field values to an ES request with those field values" in {
        val corpexRequest = CorpexSearchRequest( page = Some( 1 ),
                                                 pageSize = Some( 10 ),
                                                 fields = Some( List( "cdr.content_type", "cdr.capture_source", "cdr.extracted_metadata.Description", "word_count" ) ),
                                                 queries = Some( List() ) )

        val esRequest = conv.corpexRequestToEsRequest( corpexRequest )

        val storedFields : List[ String ] = esRequest.storedFields.get
        storedFields.length shouldBe 1
        storedFields should contain( "extracted_text.length" )
        val fields : List[ String ] = esRequest.source.get
        fields.length shouldBe 3
        fields should contain( "content_type" )
        fields should contain( "capture_source" )
        fields should contain( "extracted_metadata.Description" )
    }

    it should "convert a valid flat CorpexSearchRequest to a valid EsSearchRequest" in {
        val corpexTextQuery = CorpexTextQuery( boolType = BoolType.MUST,
                                               queriedFields = List( "cdr.extracted_text" ),
                                               queryString = "ethiopia +gambella" )

        val corpexDateQuery = CorpexCdrDateQuery( boolType = BoolType.MUST,
                                                  queriedFields = List( "cdr.extracted_metadata.CreationDate" ),
                                                  dateLo = MultiValue( long = Some( 1490745600L ) ),
                                                  dateHi = MultiValue( long = Some( 1490792399L ) ) )

        val corpexRequest = CorpexSearchRequest( page = Some( 1 ),
                                                 pageSize = Some( 10 ),
                                                 queries = Some( List( corpexTextQuery, corpexDateQuery ) ) )

        val esRequest = conv.corpexRequestToEsRequest( corpexRequest )

        esRequest.from.get shouldBe 10
        esRequest.size.get shouldBe 10
        esRequest.source.get.length shouldBe 2
        esRequest.source.get should contain( "document_id" )
        esRequest.source.get should contain( "extracted_metadata.Title" )
        esRequest.query.rangeQuery.isDefined shouldBe false
        esRequest.query.matchQuery.isDefined shouldBe false
        esRequest.query.termQuery.isDefined shouldBe false
        esRequest.query.queryStringQuery.isDefined shouldBe false
        esRequest.query.boolQuery.isDefined shouldBe true

        val boolQuery = esRequest.query.boolQuery.get
        boolQuery.filter.isDefined shouldBe false
        boolQuery.mustNot.isDefined shouldBe false
        boolQuery.should.isDefined shouldBe false
        boolQuery.must.isDefined shouldBe true
        boolQuery.must.get.length shouldBe 2
        boolQuery.must.get.head.queryStringQuery.get.query shouldBe "ethiopia +gambella"
        boolQuery.must.get.filter( _.rangeQuery.isDefined ).head.rangeQuery.get( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].gte.get shouldBe 1490745600L
        boolQuery.must.get.filter( _.rangeQuery.isDefined ).head.rangeQuery.get( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].lte.get shouldBe 1490792399L
        boolQuery.must.get.filter( _.rangeQuery.isDefined ).head.rangeQuery.get( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].format.get shouldBe
        "epoch_millis"
    }

    it should "convert a valid flat CorpexSearchRequest with sort keys to a valid EsSearchRequest" in {
        val corpexTextQuery = CorpexTextQuery( boolType = BoolType.MUST,
                                               queriedFields = List( "cdr.extracted_text" ),
                                               queryString = "ethiopia +gambella" )

        val corpexDateQuery = CorpexCdrDateQuery( boolType = BoolType.MUST,
                                                  queriedFields = List( "cdr.extracted_metadata.CreationDate" ),
                                                  dateLo = MultiValue( long = Some( 1490745600L ) ),
                                                  dateHi = MultiValue( long = Some( 1490792399L ) ) )

        val corpexRequest = CorpexSearchRequest( page = Some( 1 ),
                                                 pageSize = Some( 10 ),
                                                 sort = Some( List( CorpexSortKey( Some( SortType.ASC ) ),
                                                                    CorpexSortKey( None, Some( "cdr.extracted_metadata.CreationDate" ) ),
                                                                    CorpexSortKey( None, Some( "cdr.content_type" ), Some( SortType.DESC ) ) ) ),
                                                 queries = Some( List( corpexTextQuery, corpexDateQuery ) ) )

        val esRequest = conv.corpexRequestToEsRequest( corpexRequest )

        esRequest.sort.get.head.field shouldBe "_score"
        esRequest.sort.get.head.order.get shouldBe SortType.ASC
        esRequest.sort.get( 1 ).field shouldBe "extracted_metadata.CreationDate"
        esRequest.sort.get( 1 ).order shouldBe None
        esRequest.sort.get( 2 ).field shouldBe "content_type.term"
        esRequest.sort.get( 2 ).order.get shouldBe SortType.DESC

        esRequest.from.get shouldBe 10
        esRequest.size.get shouldBe 10
        esRequest.source.get.length shouldBe 2
        esRequest.source.get should contain( "document_id" )
        esRequest.source.get should contain( "extracted_metadata.Title" )
        esRequest.query.rangeQuery.isDefined shouldBe false
        esRequest.query.matchQuery.isDefined shouldBe false
        esRequest.query.termQuery.isDefined shouldBe false
        esRequest.query.queryStringQuery.isDefined shouldBe false
        esRequest.query.boolQuery.isDefined shouldBe true

        val boolQuery = esRequest.query.boolQuery.get
        boolQuery.filter.isDefined shouldBe false
        boolQuery.mustNot.isDefined shouldBe false
        boolQuery.should.isDefined shouldBe false
        boolQuery.must.isDefined shouldBe true
        boolQuery.must.get.length shouldBe 2
        boolQuery.must.get.head.queryStringQuery.get.query shouldBe "ethiopia +gambella"
        boolQuery.must.get.filter( _.rangeQuery.isDefined ).head.rangeQuery.get( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].gte.get shouldBe 1490745600L
        boolQuery.must.get.filter( _.rangeQuery.isDefined ).head.rangeQuery.get( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].lte.get shouldBe 1490792399L
        boolQuery.must.get.filter( _.rangeQuery.isDefined ).head.rangeQuery.get( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].format.get shouldBe "epoch_millis"
    }

    it should "convert a valid flat CorpexSearchRequest with sort keys and a tenant id to a valid EsSearchRequest" in {
        val corpexTextQuery = CorpexTextQuery( boolType = BoolType.MUST,
                                               queriedFields = List( "cdr.extracted_text" ),
                                               queryString = "ethiopia +gambella" )

        val corpexDateQuery = CorpexCdrDateQuery( boolType = BoolType.MUST,
                                                  queriedFields = List( "cdr.extracted_metadata.CreationDate" ),
                                                  dateLo = MultiValue( long = Some( 1490745600L ) ),
                                                  dateHi = MultiValue( long = Some( 1490792399L ) ) )

        val corpexRequest = CorpexSearchRequest( page = Some( 1 ),
                                                 pageSize = Some( 10 ),
                                                 sort = Some( List( CorpexSortKey( Some( SortType.ASC ) ),
                                                                    CorpexSortKey( None, Some( "cdr.extracted_metadata.CreationDate" ) ),
                                                                    CorpexSortKey( None, Some( "cdr.content_type" ), Some( SortType.DESC ) ) ) ),
                                                 queries = Some( List( corpexTextQuery, corpexDateQuery ) ),
                                                 tenantId = Some( "tenant-id" ) )

        val esRequest = conv.corpexRequestToEsRequest( corpexRequest )

        esRequest.sort.get.head.field shouldBe "_score"
        esRequest.sort.get.head.order.get shouldBe SortType.ASC
        esRequest.sort.get( 1 ).field shouldBe "extracted_metadata.CreationDate"
        esRequest.sort.get( 1 ).order shouldBe None
        esRequest.sort.get( 2 ).field shouldBe "content_type.term"
        esRequest.sort.get( 2 ).order.get shouldBe SortType.DESC

        esRequest.from.get shouldBe 10
        esRequest.size.get shouldBe 10
        esRequest.source.get.length shouldBe 2
        esRequest.source.get should contain( "document_id" )
        esRequest.source.get should contain( "extracted_metadata.Title" )
        esRequest.query.rangeQuery.isDefined shouldBe false
        esRequest.query.matchQuery.isDefined shouldBe false
        esRequest.query.termQuery.isDefined shouldBe false
        esRequest.query.queryStringQuery.isDefined shouldBe false
        esRequest.query.boolQuery.isDefined shouldBe true

        val boolQuery = esRequest.query.boolQuery.get
        boolQuery.filter.isDefined shouldBe true
        boolQuery.filter.get.length shouldBe 1
        boolQuery.filter.get.head.termQuery.get( "tenants" ).value shouldBe "tenant-id"
        boolQuery.mustNot.isDefined shouldBe false
        boolQuery.should.isDefined shouldBe false
        boolQuery.must.isDefined shouldBe true
        boolQuery.must.get.length shouldBe 2
        boolQuery.must.get.head.queryStringQuery.get.query shouldBe "ethiopia +gambella"
        boolQuery.must.get.filter( _.rangeQuery.isDefined ).head.rangeQuery.get( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].gte.get shouldBe 1490745600L
        boolQuery.must.get.filter( _.rangeQuery.isDefined ).head.rangeQuery.get( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].lte.get shouldBe 1490792399L
        boolQuery.must.get.filter( _.rangeQuery.isDefined ).head.rangeQuery.get( "extracted_metadata.CreationDate" ).asInstanceOf[ LongRangeQuery ].format.get shouldBe "epoch_millis"
    }

    it should "throw a bad request body exception for a CorpexSearchRequest with an invalid CorpexSearchQuery" in {
        an[ InvalidSearchQueryException ] should be thrownBy {
            val corpexTextQuery = CorpexTextQuery( boolType = BoolType.MUST,
                                                   queriedFields = List( "cdr.timestamp" ),
                                                   queryString = "some query" )

            val corpexDateQuery = CorpexCdrDateQuery( boolType = BoolType.MUST,
                                                      queriedFields = List( "extracted_metadata.CreationDate" ),
                                                      dateLo = MultiValue( long = Some( 1490745600L ) ),
                                                      dateHi = MultiValue( long = Some( 1490792399L ) ) )

            val corpexRequest = CorpexSearchRequest( page = Some( 1 ),
                                                     pageSize = Some( 10 ),
                                                     queries = Some( List( corpexTextQuery, corpexDateQuery ) ) )

            conv.corpexRequestToEsRequest( corpexRequest )
        }
    }

    behavior of "EsCorpexConvert.esResponseToCorpexResults"

    it should "return CorpexSearchResults for a valid EsSearchResults" in {
        val esHitsHits = List(
            EsResponseResult( "cdr_search", "_doc", "sijdfhsdjkfhsd", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "sijdfhsdjkfhsd", annotations = List.empty, timestamp = null,
                                                                                                        extractedMetadata = DartMetadataDto( title = "Some Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "dyjfghjfdhdtrht", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "dyjfghjfdhdtrht", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Second Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "teruyrtyetyertt", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "teruyrtyetyertt", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Third Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "dghdfghdfghdfgdh", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "dghdfghdfghdfgdh", annotations = List.empty, timestamp = null,
                                                                                                          extractedMetadata = DartMetadataDto( title = "Fourth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "cnbvcvbncvbnbvn", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "cnbvcvbncvbnbvn", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Fifth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "iupypioopuioyyi", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "iupypioopuioyyi", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Sixth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "qwerqertweerwf", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "qwerqertweerwf", annotations = List.empty, timestamp = null,
                                                                                                        extractedMetadata = DartMetadataDto( title = "Seventh Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "dfgjhdghdfghfghs", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "dfgjhdghdfghfghs", annotations = List.empty, timestamp = null,
                                                                                                          extractedMetadata = DartMetadataDto( title = "Eighth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "uopiwertfdghvcb", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "uopiwertfdghvcb", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Ninth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "bnmqwetryiopfdg", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( Some( List( "tenant-1", "tenant-2" ) ) ), cdrIn = DartCdrDocumentDto( documentId = "bnmqwetryiopfdg", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Tenth Title" ) ) ) ),
            )
        val esHits = EsResponseHits( EsResponseHitsTotal( 10000, exact = false ), 2.0435, esHitsHits )
        val esResponse = EsSearchResponse( hits = esHits, took = 23, timedOut = false )

        val corpexRequest = CorpexSearchRequest( queries = Some( List() ) )

        val corpexRes = conv.esResponseToCorpexResults( esResponse, corpexRequest )

        corpexRes.page.isDefined shouldBe false
        corpexRes.numResults shouldBe 10000
        corpexRes.exactNum shouldBe false
        corpexRes.pageSize.isDefined shouldBe false
        corpexRes.numPages.isDefined shouldBe false
        val allRes : List[ CorpexSingleResult ] = corpexRes.results.get
        allRes.length shouldBe 10
        allRes.forall( _.esScore.get == 2.0435 ) shouldBe true
        println( allRes )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Some Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Second Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Third Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Fourth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Fifth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Sixth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Seventh Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Eighth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Ninth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Tenth Title" )
    }

    it should "return CorpexSearchResults for a valid EsSearchResults with page, numPages, and resultsPerPage" in {
        val esHitsHits = List(
            EsResponseResult( "cdr_search", "_doc", "sijdfhsdjkfhsd", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "sijdfhsdjkfhsd", annotations = List.empty, timestamp = null,
                                                                                                                                                                 extractedMetadata = DartMetadataDto( title = "Some Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "dyjfghjfdhdtrht", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "dyjfghjfdhdtrht", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Second Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "teruyrtyetyertt", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "teruyrtyetyertt", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Third Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "dghdfghdfghdfgdh", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "dghdfghdfghdfgdh", annotations = List.empty, timestamp =
              null,
                                                                                                          extractedMetadata = DartMetadataDto( title = "Fourth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "cnbvcvbncvbnbvn", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "cnbvcvbncvbnbvn", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Fifth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "iupypioopuioyyi", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "iupypioopuioyyi", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Sixth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "qwerqertweerwf", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "qwerqertweerwf", annotations = List.empty, timestamp = null,
                                                                                                        extractedMetadata = DartMetadataDto( title = "Seventh Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "dfgjhdghdfghfghs", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "dfgjhdghdfghfghs", annotations = List.empty, timestamp = null,
                                                                                                          extractedMetadata = DartMetadataDto( title = "Eighth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "uopiwertfdghvcb", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "uopiwertfdghvcb", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Ninth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "bnmqwetryiopfdg", 2.0435, None, DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "bnmqwetryiopfdg", annotations = List.empty, timestamp = null,
                                                                                                         extractedMetadata = DartMetadataDto( title = "Tenth Title" ) ) ) ),
            )
        val esHits = EsResponseHits( EsResponseHitsTotal( 10000, exact = false ), 2.0435, esHitsHits )
        val esResponse = EsSearchResponse( hits = esHits, took = 23, timedOut = false )

        val corpexRequest = CorpexSearchRequest( Some( 3 ), Some( 10 ), queries = Some( List() ) )

        val corpexRes = conv.esResponseToCorpexResults( esResponse, corpexRequest )

        corpexRes.page.get shouldBe 3
        corpexRes.numResults shouldBe 10000
        corpexRes.exactNum shouldBe false
        corpexRes.pageSize.get shouldBe 10
        corpexRes.numPages.get shouldBe 1000
        val allRes : List[ CorpexSingleResult ] = corpexRes.results.get
        allRes.length shouldBe 10
        allRes.forall( _.esScore.get == 2.0435 ) shouldBe true
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Some Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Second Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Third Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Fourth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Fifth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Sixth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Seventh Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Eighth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Ninth Title" )
    }

    it should "return CorpexSearchResults for a valid EsSearchResults with word_count" in {
        val esHitsHits = List(
            EsResponseResult( "cdr_search", "_doc", "sijdfhsdjkfhsd", 2.0435, Some( Map( "extracted_text.length" -> List( MultiValue( long = Some( 513 ) ) ) ) ),
                              DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "sijdfhsdjkfhsd", annotations = List.empty, timestamp = null,
                                                  extractedMetadata = DartMetadataDto( title = "Some Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "dyjfghjfdhdtrht", 2.0435, Some( Map( "extracted_text.length" -> List( MultiValue( long = Some( 513 ) ) ) ) ),
                              DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "dyjfghjfdhdtrht", annotations = List.empty, timestamp = null,
                                                  extractedMetadata = DartMetadataDto( title = "Second Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "teruyrtyetyertt", 2.0435, Some( Map( "extracted_text.length" -> List( MultiValue( long = Some( 513 ) ) ) ) ),
                              DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "teruyrtyetyertt", annotations = List.empty, timestamp = null,
                                                  extractedMetadata = DartMetadataDto( title = "Third Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "dghdfghdfghdfgdh", 2.0435, Some( Map( "extracted_text.length" -> List( MultiValue( long = Some( 513 ) ) ) ) ),
                              DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "dghdfghdfghdfgdh", annotations = List.empty, timestamp = null,
                                                  extractedMetadata = DartMetadataDto( title = "Fourth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "cnbvcvbncvbnbvn", 2.0435, Some( Map( "extracted_text.length" -> List( MultiValue( long = Some( 513 ) ) ) ) ),
                              DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "cnbvcvbncvbnbvn", annotations = List.empty, timestamp = null,
                                                  extractedMetadata = DartMetadataDto( title = "Fifth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "iupypioopuioyyi", 2.0435, Some( Map( "extracted_text.length" -> List( MultiValue( long = Some( 513 ) ) ) ) ),
                              DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "iupypioopuioyyi", annotations = List.empty, timestamp = null,
                                                  extractedMetadata = DartMetadataDto( title = "Sixth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "qwerqertweerwf", 2.0435, Some( Map( "extracted_text.length" -> List( MultiValue( long = Some( 513 ) ) ) ) ),
                              DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "qwerqertweerwf", annotations = List.empty, timestamp = null,
                                                  extractedMetadata = DartMetadataDto( title = "Seventh Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "dfgjhdghdfghfghs", 2.0435, Some( Map( "extracted_text.length" -> List( MultiValue( long = Some( 513 ) ) ) ) ),
                              DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "dfgjhdghdfghfghs", annotations = List.empty, timestamp = null,
                                                  extractedMetadata = DartMetadataDto( title = "Eighth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "uopiwertfdghvcb", 2.0435, Some( Map( "extracted_text.length" -> List( MultiValue( long = Some( 513 ) ) ) ) ),
                              DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "uopiwertfdghvcb", annotations = List.empty, timestamp = null,
                                                  extractedMetadata = DartMetadataDto( title = "Ninth Title" ) ) ) ),
            EsResponseResult( "cdr_search", "_doc", "bnmqwetryiopfdg", 2.0435, Some( Map( "extracted_text.length" -> List( MultiValue( long = Some( 513 ) ) ) ) ),
                              DartEsDocument( privateFieldsIn = DartPrivateFields( None ), cdrIn = DartCdrDocumentDto( documentId = "bnmqwetryiopfdg", annotations = List.empty, timestamp = null,
                                                  extractedMetadata = DartMetadataDto( title = "Tenth Title" ) ) ) ),
            )

        val esHits = EsResponseHits( EsResponseHitsTotal( 10000, exact = false ), 2.0435, esHitsHits )
        val esResponse = EsSearchResponse( hits = esHits, took = 23, timedOut = false )

        val corpexRequest = CorpexSearchRequest( Some( 3 ), Some( 10 ), queries = Some( List() ) )

        val corpexRes = conv.esResponseToCorpexResults( esResponse, corpexRequest )

        corpexRes.page.get shouldBe 3
        corpexRes.numResults shouldBe 10000
        corpexRes.exactNum shouldBe false
        corpexRes.pageSize.get shouldBe 10
        corpexRes.numPages.get shouldBe 1000
        val allRes : List[ CorpexSingleResult ] = corpexRes.results.get
        allRes.length shouldBe 10
        allRes.forall( _.esScore.get == 2.0435 ) shouldBe true
        allRes.foreach( res => res.wordCount shouldBe Some( 513 ) )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Some Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Second Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Third Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Fourth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Fifth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Sixth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Seventh Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Eighth Title" )
        allRes.map( _.cdr.extractedMetadata.title ) should contain( "Ninth Title" )
    }

    it should "return a CorpexSearchResults object for search results from a real ES response" in {
        val corpexRequest = CorpexSearchRequest( Some( 3 ),
                                                 Some( 10 ),
                                                 queries = Some( List() ),
                                                 aggs = Some( Map( "AGGLABEL" -> CorpexTagValuesAggQuery( tagId = TagIds.QNTFY_NER.apiLabel,
                                                                                                          tagTypes = Some( List( EntityTagTypes.GPE.apiLabel ) ) ) ) ) )

        val json = Resource.getAsString( "es_response_tag_agg.json" )

        val esResponse = Mapper.unmarshal( json, classOf[ EsSearchResponse ] )

        val corpexResponse = conv.esResponseToCorpexResults( esResponse, corpexRequest )

        val agg : List[ Count ] = corpexResponse.aggregations.get( "AGGLABEL" )

        agg.size shouldBe 5000
        agg.head match {
            case ValueCount( count : Int, value : String ) => print( "" )
            case _ => fail( "not a ValueCount!" )
        }
    }

}
