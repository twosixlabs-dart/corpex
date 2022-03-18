package com.twosixlabs.dart.corpex.services.search.es

import java.time.OffsetDateTime
import com.twosixlabs.dart.corpex.api.configuration.annotations.{ FacetId, TagId, TagType }
import com.twosixlabs.dart.corpex.api.enums.{ BoolType, SortType }
import com.twosixlabs.dart.corpex.api.models.MultiValue
import com.twosixlabs.dart.corpex.services.search.es.models.EsSortKey
import com.twosixlabs.dart.corpex.services.search.es.models.{ AggBucketSort, AggQuery, BoolQuery, DoubleRangeQuery, EsAggQuery, EsQuery, EsSortKey, LongAggQuery, LongRangeQuery, MatchQuery, NestedQuery, QueryStringQuery, TermQuery }
import com.twosixlabs.dart.corpex.tools.ParseDate
import com.twosixlabs.dart.exceptions.BadRequestBodyException
import org.slf4j.{ Logger, LoggerFactory }

object QueryGenerators {

    val LOG : Logger = LoggerFactory.getLogger( getClass )


    def makeTermAgg( field : String, size : Option[ Int ], queryString : Option[ String ] = None, order : Option[ EsSortKey ] = None ) : EsAggQuery = {
        val filter = queryString.map( qs => makeQueryStringQuery( List( field ), qs ) )
        val agg = EsAggQuery( terms = Some( AggQuery( Some( field + ".term" ), size = size, order = order ) ) )

        filter match {
            case Some( f : EsQuery ) => makeFilterAgg( f, "docs", agg )
            case None => agg
        }
    }

    def makeIntAgg( field : String, interval : Long, lo : Option[ Long ], hi : Option[ Long ] ) : EsAggQuery = {
        val agg : EsAggQuery = EsAggQuery( histogram = Some( LongAggQuery( field = Some( field ), interval = Some( interval ) ) ) )

        val filter = if ( lo.isEmpty && hi.isEmpty ) None else Some( makeIntRangeQuery( field = field, lo, hi ) )

        filter match {
            case Some( f : EsQuery ) => makeFilterAgg( f, "docs", agg )
            case None => agg
        }
    }

    def makeDateAgg( field : String, interval : MultiValue, lo : Option[ OffsetDateTime ] = None, hi : Option[ OffsetDateTime ] = None ) : EsAggQuery = {
        val IntPattern = "([0-9]+)([mshdwMqy]+)".r
        val CharPattern = "([mshdwMqy]+)".r
        val fixedInts = Set( "ms", "s", "m", "h", "d" )
        val calendarInts = Set( "m", "h", "d", "w", "M", "q", "y" )

        val esDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

        val agg : EsAggQuery = interval.get() match {
            case stringInt : String =>
                stringInt match {
                    case IntPattern( int, chars ) =>
                        if ( int.toInt > 1 ) {
                            if ( fixedInts.contains( chars ) ) EsAggQuery( date = Some( AggQuery( Some( field ), fixedInt = Some( stringInt ), format = Some( esDateFormat ) ) ) )
                            else EsAggQuery( date = Some( AggQuery( Some( field ), fixedInt = Some( ParseDate.getMsIntervalFromString( stringInt ) + "ms" ), format = Some(
                                esDateFormat ) ) ) )
                        } else {
                            if ( calendarInts.contains( chars ) ) EsAggQuery( date = Some( AggQuery( Some( field ), calendarInt = Some( stringInt ), format = Some( esDateFormat
                                                                                                                                                                    ) ) ) )
                            else if ( fixedInts.contains( chars ) ) EsAggQuery( date = Some( AggQuery( Some( field ), fixedInt = Some( stringInt ), format = Some( esDateFormat )
                                                                                                       ) ) )
                            else if ( chars == "ms" ) EsAggQuery( date = Some( AggQuery( Some( field ), fixedInt = Some( s"${ParseDate.getMsIntervalFromString( stringInt )}ms" )
                                                                                         , format = Some( esDateFormat ) ) ) )
                            else throw new BadRequestBodyException( "Unable to parse bucket_size string" )
                        }
                    case CharPattern( chars ) =>
                        if ( calendarInts.contains( chars ) ) EsAggQuery( date = Some( AggQuery( Some( field ), calendarInt = Some( 1 + chars ), format = Some( esDateFormat ) ) ) )
                        else if ( fixedInts.contains( chars ) ) EsAggQuery( date = Some( AggQuery( Some( field ), fixedInt = Some( 1 + chars ), format = Some( esDateFormat ) ) ) )
                        else throw new BadRequestBodyException( "Unable to parse bucket_size string" )

                    case _ => throw new Exception( "Unable to parse bucket_size string" )
                }

            case ms : Long =>
                EsAggQuery( date = Some( AggQuery( Some( field ), fixedInt = Some( ms + "ms" ), format = Some( esDateFormat ) ) ) )
        }

        val filter =
            if ( lo.isEmpty && hi.isEmpty ) None
            else Some( makeDateRangeQuery( field = field, lo, hi ) )

        filter match {
            case Some( f : EsQuery ) => makeFilterAgg( f, "docs", agg )
            case None => agg
        }
    }

    def makeNestedAggQuery( path : String, nestAggLabel : String, nestedAgg : EsAggQuery ) : EsAggQuery = {
        EsAggQuery( nested = Some( AggQuery( path = Some( path ) ) ), aggs = Some( Map( nestAggLabel -> nestedAgg ) ) )
    }

    def makeFilterAgg( filter : EsQuery, aggLabel : String, filterAgg : EsAggQuery ) : EsAggQuery = {
        EsAggQuery( filter = Some( filter ), aggs = Some( Map( aggLabel -> filterAgg ) ) )
    }

    def makeRevNestedAgg( path : Option[ String ] = None ) : EsAggQuery = {
        EsAggQuery( reverseNested = Some( AggQuery( path = path ) ) )
    }

    def makeTagTypesAgg( aggLabel : String,
                         size : Option[ Int ],
                         tagId : TagId,
                         typesQuery : Option[ String ] = None ) : EsAggQuery = {

        val valueAggTermOrder = EsSortKey( s"${aggLabel}_docs.doc_count", Some( SortType.DESC ) )
        val typeAggTerm = makeTermAgg( "annotations.content.tag", size, order = Some( valueAggTermOrder ) )
        val typeAgg = typeAggTerm.copy( aggs = Some( Map( aggLabel + "_docs" -> makeRevNestedAgg() ) ) )

        val tagFilterTermQuery : Option[ EsQuery ] =
            typesQuery.map( typesQueryStr => makeTermQuery( "annotations.content.tag", typesQueryStr ) )

        val tagFilter : Option[ EsQuery ] = typesQuery match {
            case None => tagFilterTermQuery
            case Some( qs ) =>
                val queryStringQuery = makeQueryStringQuery( List( "annotations.content.value" ), qs )
                if ( tagFilterTermQuery.isEmpty ) Some( queryStringQuery )
                else Some( makeBoolQuery( filter = List( tagFilterTermQuery, Some( queryStringQuery ) ).flatten ) )
        }

        val tagFilterAgg =
            if ( tagFilter.isDefined ) makeFilterAgg( tagFilter.get, aggLabel + "_terms", typeAgg )
            else typeAgg

        val contentAgg = makeNestedAggQuery( "annotations.content", aggLabel + "_tag_filter", tagFilterAgg )

        val labelFilter = makeTermQuery( "annotations.label", tagId.cdrLabel )

        val labelFilterAgg : EsAggQuery = makeFilterAgg( labelFilter, aggLabel + "_content", contentAgg )

        makeNestedAggQuery( "annotations", aggLabel + "_label_filter", labelFilterAgg )
    }

    def makeTagValuesAgg( aggLabel : String,
                          size : Option[ Int ],
                          tagId : TagId,
                          tagTypes : Option[ List[ String ] ] = None,
                          typesQuery : Option[ String ] = None,
                          valuesQuery : Option[ String ] = None ) : EsAggQuery = {

        val valueAggTermSortKey = EsSortKey( s"${aggLabel}_docs.doc_count", Some( SortType.DESC ) )
        val valueAggTerm = makeTermAgg( "annotations.content.value", size, order = Some( valueAggTermSortKey ) )
        val valueAgg = valueAggTerm.copy( aggs = Some( Map( aggLabel + "_docs" -> makeRevNestedAgg() ) ) )

        val tagFilterTypesFilter : Option[ EsQuery ] =
            if ( tagTypes.isDefined ) tagTypes.map( tagTypesStr => makeBoolQuery( should = tagTypesStr.map( tagType =>  makeTermQuery( "annotations.content.tag", tagType ) ) ) )
            else if ( typesQuery.isDefined ) typesQuery.map( query => makeQueryStringQuery( List( "annotations.content.tag" ), query ) )
            else None

        val tagFilterValueQuery : Option[ EsQuery ] =
            valuesQuery.map( ( query : String ) => makeQueryStringQuery( List( "annotations.content.value" ), query ) )

        val tagFilter =
            if ( tagFilterTypesFilter.isDefined && tagFilterValueQuery.isEmpty ) tagFilterTypesFilter
            else if ( tagFilterTypesFilter.isEmpty && tagFilterValueQuery.isDefined ) tagFilterValueQuery
            else if ( tagFilterTypesFilter.isDefined && tagFilterValueQuery.isDefined ) Some( makeBoolQuery( filter = List( tagFilterTypesFilter, tagFilterValueQuery ).flatten ) )
            else None

        val tagFilterAgg =
            if ( tagFilter.isDefined ) makeFilterAgg( tagFilter.get, aggLabel + "_terms", valueAgg )
            else valueAgg

        val contentAgg = makeNestedAggQuery( "annotations.content", aggLabel + "_tag_filter", tagFilterAgg )

        val labelFilter = makeTermQuery( "annotations.label", tagId.cdrLabel )

        val labelFilterAgg : EsAggQuery = makeFilterAgg( labelFilter, aggLabel + "_content", contentAgg )

        makeNestedAggQuery( "annotations", aggLabel + "_label_filter", labelFilterAgg )
    }

    def makeFacetAgg( aggLabel : String, facetId : FacetId, size : Option[ Int ], queryString : Option[ String ] = None, scoreLo : Option[ Double ] = None,
                      scoreHi : Option[ Double ] = None ) : EsAggQuery = {

        val queryStringQuery = queryString.map( qs => makeQueryStringQuery( List( "annotations.content.value" ), qs ) ).toList
        val scoreRangeQuery : List[ EsQuery ] = if ( scoreLo.isEmpty && scoreHi.isEmpty ) List() else {
            List( makeConfRangeQuery( "annotations.content.score", scoreLo, scoreHi ) )
        }

        val filters = List( queryStringQuery, scoreRangeQuery ).flatten

        val filter = filters match {
            case List( _, _ ) => Some( makeBoolQuery( filter = filters ) )
            case List( _ ) => Some( filters.head )
            case List() => None
        }

        val valueAggTerm = makeTermAgg( "annotations.content.value", size )
        val valueAggTermWithDocs = valueAggTerm.copy( aggs = Some( Map( aggLabel + "_docs" -> makeRevNestedAgg() ) ) )

        val valueAgg = if ( filter.isEmpty ) valueAggTermWithDocs else makeFilterAgg( filter.get, aggLabel + "_facet_filter", valueAggTermWithDocs )

        val contentAgg = makeNestedAggQuery( "annotations.content", aggLabel + "_terms", valueAgg )

        val labelFilter = makeTermQuery( "annotations.label", facetId.cdrLabel )

        val labelFilterAgg : EsAggQuery = makeFilterAgg( labelFilter, aggLabel + "_content", contentAgg )

        makeNestedAggQuery( "annotations", aggLabel + "_label_filter", labelFilterAgg )
    }

    def makeBucketAggSort( path : String, order : Option[ SortType.SortType ], size : Option[ Int ] ) : EsAggQuery = {
        val bucketSort = AggBucketSort( size, Some( List( EsSortKey( path, order ) ) ) )
        EsAggQuery( bucketSort = Some( bucketSort ) )
    }

    def makeAvgAgg( field : String ) : EsAggQuery = {
        val avgAgg = AggQuery( field = Some( field ) )
        EsAggQuery( avg = Some( avgAgg ) )
    }

    def makeFacetScoreAgg( aggLabel : String, facetId : FacetId, valueQuery : Option[ String ], size : Option[ Int ] ) : EsAggQuery = {
        val valueFilter = valueQuery.map( qs => makeQueryStringQuery( List( "annotations.content.value" ), qs ) )

        val valueAggTerm = makeTermAgg( "annotations.content.value", Some( 10000 ) )
        val valueAggTermWithDocs = valueAggTerm.copy( aggs = Some(
            Map(
                aggLabel + "_docs" -> makeRevNestedAgg(),
                aggLabel + "_score" -> makeAvgAgg( "annotations.content.score" ),
                aggLabel + "_sort" -> makeBucketAggSort( aggLabel + "_score", Some( SortType.DESC ), size ),
            )
        ) )

        val valueAgg = if ( valueFilter.isEmpty ) valueAggTermWithDocs else {
            makeFilterAgg( valueFilter.get, aggLabel + "_facet_filter", valueAggTermWithDocs )
        }

        val contentAgg = makeNestedAggQuery( "annotations.content", aggLabel + "_terms", valueAgg )

        val labelFilter = makeTermQuery( "annotations.label", facetId.cdrLabel )

        val labelFilterAgg : EsAggQuery = makeFilterAgg( labelFilter, aggLabel + "_content", contentAgg )

        makeNestedAggQuery( "annotations", aggLabel + "_label_filter", labelFilterAgg )

    }

    def makeQueryStringQuery( fields : List[ String ], queryString : String, valBool : Option[ String ] = None ) : EsQuery = {
        EsQuery( queryStringQuery = Some( QueryStringQuery( fields = fields, query = queryString ) ) )
    }

    def makeTermQuery( field : String, value : String ) : EsQuery = {
        EsQuery( termQuery = Some( Map( field + ".term" -> TermQuery( value ) ) ) )
    }

    def makeMatchQuery( field : String, value : String, valBool : Option[ String ] = None ) : EsQuery = {
        EsQuery( matchQuery = Some( Map( field -> MatchQuery( value, valBool ) ) ) )
    }

    def makeBoolQuery( must : List[ EsQuery ] = List.empty, should : List[ EsQuery ] = List.empty, filter : List[ EsQuery ] = List.empty,
                       mustNot : List[ EsQuery ] = List.empty ) : EsQuery = {
        EsQuery( boolQuery = Some( BoolQuery(
            must = if ( must.nonEmpty ) Some( must ) else None,
            should = if ( should.nonEmpty ) Some( should ) else None,
            filter = if ( filter.nonEmpty ) Some( filter ) else None,
            mustNot = if ( mustNot.nonEmpty ) Some( mustNot ) else None
            ) ) )
    }

    def makeBoolQuerySingleType( boolType : BoolType.BoolType, queries : List[ EsQuery ] ) : EsQuery = {
        makeBoolQuery(
            must = if ( boolType == BoolType.MUST ) queries else Nil,
            should = if ( boolType == BoolType.SHOULD ) queries else Nil,
            filter = if ( boolType == BoolType.FILTER ) queries else Nil,
            mustNot = if ( boolType == BoolType.MUST_NOT ) queries else Nil
        )
    }

    def addToBoolQuery( boolQuery : EsQuery, boolType: BoolType.BoolType, queries : List[ EsQuery ] ) : EsQuery = {
        boolQuery.copy( boolQuery = boolQuery.boolQuery.map( bq => bq.copy(
            must = if ( boolType == BoolType.MUST ) Some( bq.must.map( _ ++ queries ).getOrElse( queries ) ) else bq.must,
            should = if ( boolType == BoolType.SHOULD ) Some( bq.should.map( _ ++ queries ).getOrElse( queries ) ) else bq.should,
            filter = if ( boolType == BoolType.FILTER ) Some( bq.filter.map( _ ++ queries ).getOrElse( queries ) ) else bq.filter,
            mustNot = if ( boolType == BoolType.MUST_NOT ) Some( bq.mustNot.map( _ ++ queries ).getOrElse( queries ) ) else bq.mustNot,
        ) ) )
    }

    def makeNestedQuery( path : String, query : EsQuery ) : EsQuery = {
        EsQuery( nestedQuery = Some( NestedQuery( path = path, query = query ) ) )
    }

    def makeDateRangeQuery( field : String, gte : Option[ OffsetDateTime ] = None, lte : Option[ OffsetDateTime ] = None ) : EsQuery = {
        EsQuery( rangeQuery = Some( Map( field -> LongRangeQuery( gte.map( _.toInstant.toEpochMilli ), lte.map( _.toInstant.toEpochMilli ), Some( "epoch_millis" ) ) ) ) )
    }

    def makeIntRangeQuery( field : String, gte : Option[ Long ] = None, lte : Option[ Long ] = None ) : EsQuery = {
        EsQuery( rangeQuery = Some( Map( field -> LongRangeQuery( gte, lte, None ) ) ) )
    }

    def makeConfRangeQuery( field : String, gte : Option[ Double ] = None, lte : Option[ Double ] = None, format : Option[ String ] = None ) : EsQuery = {
        EsQuery( rangeQuery = Some( Map( field -> DoubleRangeQuery( gte, lte, format ) ) ) )
    }

    def makeTagQuery( tagId : TagId,
                      tagTypes : Option[ List[ TagType ] ] = None,
                      tagTypesExact : Option[ List [ String ] ] = None,
                      tagTypesQuery : Option[ String ] = None,
                      typesBoolType : Option[ BoolType.Value ] = None,
                      tagValues : Option[ List[ String ] ] = None,
                      tagValuesQuery : Option[ String ] = None,
                      valuesBoolType : Option[ BoolType.Value ] = None ) : EsQuery = {

        val idQuery = makeTermQuery( "annotations.label", tagId.cdrLabel )
        val typeQuery : Option[ EsQuery ] =
            if ( tagTypes.isDefined && tagTypes.get.nonEmpty )
                if ( tagTypes.get.length == 1 ) Some( makeTermQuery( "annotations.content.tag", tagTypes.get.head.cdrLabel ) )
                else Some( makeBoolQuerySingleType( typesBoolType.getOrElse( BoolType.SHOULD ), tagTypes.get.map( tagType => makeTermQuery( "annotations.content.tag", tagType.cdrLabel ) ) ) )
            else if ( tagTypesExact.isDefined && tagTypesExact.get.nonEmpty )
                if ( tagTypesExact.get.length == 1 ) Some( makeTermQuery( "annotations.content.tag", tagTypesExact.get.head ) )
                else Some( makeBoolQuerySingleType( typesBoolType.getOrElse( BoolType.SHOULD ), tagTypesExact.get.map( tagTypeStr => makeTermQuery( "annotations.content.tag", tagTypeStr ) ) ) )
            else if ( tagTypesQuery.isDefined ) Some( makeQueryStringQuery( List( "annotations.content.tag" ), tagTypesQuery.get, Some( "AND" ) ) )
            else None

        val typeQsQuery : Option[ EsQuery ] = {
            val queries : List[ EsQuery ] =
                List( typeQuery, tagValuesQuery.map( query => makeQueryStringQuery( List( "annotations.content.value" ), query, Some( "AND" ) ) ) ).flatten
            if ( queries.isEmpty ) None
            else Some( makeNestedQuery( path = "annotations.content", query = makeBoolQuery( must = queries ) ) )
        }

        val valuesQueries : Option[List[EsQuery ] ] = tagValues.map( _.map( value => {
            val termQuery : EsQuery = makeTermQuery( "annotations.content.value", value )
            makeNestedQuery( path = "annotations.content", query = makeBoolQuery( must = List( typeQuery, Some( termQuery ) ).flatten ) )
        } ) )

        val lowerQuery =
            if ( valuesQueries.isDefined )
                valuesQueries.map( queries => makeBoolQuerySingleType( queries = queries, boolType = valuesBoolType.getOrElse( BoolType.MUST ) ) )
            else if ( typeQsQuery.isDefined ) typeQsQuery
            else None

        val upperQuery =
            if ( lowerQuery.isDefined ) makeBoolQuery( must = List( idQuery, lowerQuery.get ) )
            else idQuery

        val annotationsQuery = makeNestedQuery( path = "annotations",
                                                query = upperQuery )

        annotationsQuery
    }

    def makeFacetQuery( facetId : FacetId,
                        query : Option[ String ] = None,
                        scoreLo : Option[ Double ] = None,
                        scoreHi : Option[ Double ] = None,
                        scoreBoolType : Option[ BoolType.BoolType ] = None,
                        facetValues : Option[ List[ String ] ] = None,
                        valuesBoolType : Option[ BoolType.Value ] = None ) : EsQuery = {

        val idQuery = makeTermQuery( "annotations.label", facetId.cdrLabel )

        val scoreQuery : Option[ EsQuery ] =
            if ( scoreLo.isDefined || scoreHi.isDefined ) Some( makeConfRangeQuery( "annotations.content.score", scoreLo, scoreHi ) )
            else None

        val queryQuery = query map { queryStr =>
            val valueQuery : EsQuery = makeQueryStringQuery( List( "annotations.content.value" ), queryStr, Some( "AND" ) )
            val nestQuery : EsQuery = scoreQuery map { ( cq : EsQuery ) =>
                addToBoolQuery( makeBoolQuery( must = List( valueQuery ) ),
                                scoreBoolType.getOrElse( BoolType.FILTER ),
                                List( cq ) )
            } getOrElse( valueQuery )

            makeNestedQuery( path = "annotations.content", query = nestQuery )
        }

        val valuesQuery = facetValues map { fv =>
            val valueQueries = fv.map( value => {
                val valueQuery = makeTermQuery( "annotations.content.value", value )
                val nestQuery = scoreQuery map { cq =>
                    addToBoolQuery( makeBoolQuery( must = List( valueQuery ) ),
                                    scoreBoolType.getOrElse( BoolType.FILTER ),
                                    List( cq ) )
                } getOrElse( valueQuery )

                makeNestedQuery( "annotations.content", query = nestQuery )
            } )

            makeBoolQuerySingleType( valuesBoolType.getOrElse( BoolType.MUST ), valueQueries )
        }

        val lowerQuery : Option[ EsQuery ] =
            if ( queryQuery.isDefined ) queryQuery
            else if ( valuesQuery.isDefined ) valuesQuery
            else None

        val upperQuery =
            if ( lowerQuery.isDefined ) makeBoolQuery( must = List( idQuery, lowerQuery.get ) )
            else idQuery

        val annotationsQuery = makeNestedQuery( path = "annotations",
                                                query = upperQuery )

        annotationsQuery
    }
}
