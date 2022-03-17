package com.twosixlabs.dart.corpex.services.es

import com.twosixlabs.dart.corpex.api.configuration.annotations.{AllTags, FacetIds, TagIds}
import com.twosixlabs.dart.corpex.api.configuration.metadata.{CdrDataId, CorpexDataId, DataIds, DataTypes}
import com.twosixlabs.dart.corpex.api.enums.BoolType
import com.twosixlabs.dart.corpex.api.models.queries.{CorpexAggQuery, CorpexBoolQuery, CorpexCdrDateQuery, CorpexFacetAggQuery, CorpexFacetScoreAggQuery, CorpexFacetQuery, CorpexFieldAggQuery, CorpexIntegerQuery, CorpexSearchQuery, CorpexTagQuery, CorpexTagTypesAggQuery, CorpexTagValuesAggQuery, CorpexTermQuery, CorpexTextQuery}
import com.twosixlabs.dart.corpex.api.models.{CorpexSearchRequest, CorpexSearchResults, CorpexSingleResult, Count, DateRangeCount, FloatRangeCount, IntRangeCount, MultiValue, ValueScore, ValueCount}
import com.twosixlabs.dart.corpex.services.es.QueryGenerators._
import com.twosixlabs.dart.corpex.services.es.models.{EsAggQuery, EsAggResult, EsQuery, EsResponseResult, EsSearchRequest, EsSearchResponse, EsSortKey, TermQuery}
import com.twosixlabs.dart.corpex.tools.{ParseDate, ParseQuery}
import com.twosixlabs.dart.exceptions.BadRequestBodyException
import org.slf4j.{Logger, LoggerFactory}

class EsCorpexConvert( defaultPageSize : Int,
                       baseFields : List[ String ],
                       defaultTextField : String ) {

    val defaultAggSize = 200

    val LOG : Logger = LoggerFactory.getLogger( getClass )

    def convertIntFieldAgg( query : CorpexFieldAggQuery, aggLabel : String ) : EsAggQuery = {
        val field = DataIds.get( query.queriedField, _.apiLabel ).get.cdrLabel.get
        val defaultBucketSize = DataIds.get( query.queriedField, _.apiLabel ).get.defaultBucketSize.get.long.get
        makeIntAgg( field,
                    query.bucketSize.long.getOrElse( defaultBucketSize ),
                    query.lo.long,
                    query.hi.long )
    }

    def convertTermFieldAgg( query : CorpexFieldAggQuery, aggLabel : String ) : EsAggQuery = {
        val field = DataIds.get( query.queriedField, _.apiLabel ).get.cdrLabel.get
        makeTermAgg( field, Some( query.size.getOrElse( defaultAggSize ) ), query.valuesQuery )
    }

    def convertDateFieldAgg( query : CorpexFieldAggQuery, aggLabel : String ) : EsAggQuery = {
        val field = DataIds.get( query.queriedField, _.apiLabel ).get.cdrLabel.get
        val bucketSize : MultiValue = if ( query.bucketSize.isDefined ) query.bucketSize else DataIds.get( query.queriedField, _.apiLabel ).get.defaultBucketSize.get
        makeDateAgg( field, bucketSize, query.dateLo, query.dateHi )
    }

    def convertTagValuesAgg( query : CorpexTagValuesAggQuery, aggLabel : String ) : EsAggQuery = {
        val tagId = TagIds.get( query.tagId, _.apiLabel ).get
        val tagTypes =
            if ( query.tagTypes.isDefined ) query.tagTypes.map( _.map( tagType => AllTags.get( tagType, _.apiLabel ).get.cdrLabel ) )
            else if ( query.tagTypesExact.isDefined ) query.tagTypes else None
        makeTagValuesAgg( aggLabel, Some( query.size.getOrElse( defaultAggSize ) ), tagId, tagTypes, query.tagTypesQuery, query.tagValuesQuery )
    }

    def convertTagTypesAgg( query : CorpexTagTypesAggQuery, aggLabel : String ) : EsAggQuery = {
        val tagId = TagIds.get( query.tagId, _.apiLabel ).get
        makeTagTypesAgg( aggLabel, Some( query.size.getOrElse( defaultAggSize ) ), tagId, query.tagTypesQuery )
    }

    def convertFacetAgg( query : CorpexFacetAggQuery, aggLabel : String ) : EsAggQuery = {
        val facetId = FacetIds.get( query.facetId, _.apiLabel ).get
        makeFacetAgg( aggLabel, facetId, Some( query.size.getOrElse( defaultAggSize ) ), query.facetValuesQuery, query.scoreLo, query.scoreHi )
    }

    def convertFacetScoreAgg( query : CorpexFacetScoreAggQuery, aggLabel : String ) : EsAggQuery = {
        val facetId = FacetIds.get( query.facetId, _.apiLabel ).get
        makeFacetScoreAgg( aggLabel, facetId, query.facetValuesQuery, query.size )
    }

    def corpexAggQueryToEsAggQuery( agg : CorpexAggQuery, aggLabel : String = "" ) : EsAggQuery = {
        agg match {
            case fieldQuery : CorpexFieldAggQuery =>
                DataIds.get( fieldQuery.queriedField, _.apiLabel ).map( _.dataType ) match {
                    case Some( DataTypes.UNUSED ) =>
                        throw new BadRequestBodyException( s"${DataTypes.UNUSED.toString} is not a valid field for aggregation" )
                    case Some( DataTypes.INT ) => convertIntFieldAgg( fieldQuery, aggLabel )
                    case Some( DataTypes.TERM ) => convertTermFieldAgg( fieldQuery, aggLabel )
                    case Some( DataTypes.DATE ) => convertDateFieldAgg( fieldQuery, aggLabel )
                    case Some( _ ) => throw new IllegalStateException( "Can't have CorpexFieldAggQuery with a DataType other than those enumerated in DataTypes" )
                    case None => throw new IllegalStateException( "Can't have CorpexFieldAggQuery without queriedField corresponding to a DataId" )
                }

            case facetQuery : CorpexFacetAggQuery => convertFacetAgg( facetQuery, aggLabel )
            case facetScoreQuery : CorpexFacetScoreAggQuery => convertFacetScoreAgg( facetScoreQuery, aggLabel )
            case tagTypesQuery : CorpexTagTypesAggQuery => convertTagTypesAgg( tagTypesQuery, aggLabel )
            case tagValuesQuery : CorpexTagValuesAggQuery => convertTagValuesAgg( tagValuesQuery, aggLabel )
        }
    }

    def corpexQueryToEsQuery( queryIn : CorpexSearchQuery ) : EsQuery = {
        queryIn match {
            case CorpexTextQuery( _, queriedFields, queryString ) =>
                val queriedFieldsEs = queriedFields.map( field => {
                    val dataId = DataIds.get( field, _.apiLabel )

                    if ( dataId.isEmpty )
                        throw new BadRequestBodyException( "queried_fields", Some( field ), s"queried_fields value ${field} is not a valid field" )

                    dataId.get.dataType match {
                        case DataTypes.TEXT => dataId.get.cdrLabel.get
                        case DataTypes.TERM => dataId.get.cdrLabel.get
                        case _ => throw new BadRequestBodyException( "queried_fields", Some( field ), s"queried_fields value ${field} is not a text field" )
                    }
                } )

                makeQueryStringQuery( queriedFieldsEs, queryString )

            case CorpexTermQuery( _, queriedField, termValues, valuesBoolType ) =>
                val queriedFieldEs = DataIds.get( queriedField, _.apiLabel ).get.cdrLabel.get

                val termQueries = termValues.map( term => {
                    makeTermQuery( queriedFieldEs, term )
                } )

                makeBoolQuerySingleType( valuesBoolType.getOrElse( BoolType.SHOULD ),
                                         termQueries )


            case query : CorpexCdrDateQuery =>
                val queriedFieldsEs = query.queriedFields.map( apiField => {
                    DataIds.get( apiField, _.apiLabel ).get.cdrLabel.get
                } )

                if ( queriedFieldsEs.length == 1 ) {
                    makeDateRangeQuery( queriedFieldsEs.head, query.dateLoDate, query.dateHiDate )
                } else {
                    val dateQueries = queriedFieldsEs.map( dateField =>
                                                               makeDateRangeQuery( dateField, query.dateLoDate, query.dateHiDate ) )
                    val datesBoolType = query.multiBoolType.getOrElse( BoolType.FILTER )

                    datesBoolType match {
                        case BoolType.MUST => makeBoolQuery( must = dateQueries )
                        case BoolType.SHOULD => makeBoolQuery( should = dateQueries )
                        case BoolType.FILTER => makeBoolQuery( filter = dateQueries )
                        case BoolType.MUST_NOT => makeBoolQuery( mustNot = dateQueries )
                    }
                }

            case CorpexIntegerQuery( _, queriedFields, multiBoolType, intHi, intLo ) =>
                val queriedFieldsEs = queriedFields.map( apiField => {
                    DataIds.get( apiField, _.apiLabel ).get.cdrLabel.get
                } )

                if ( queriedFields.length == 1 ) {
                    makeIntRangeQuery( queriedFieldsEs.head, intLo, intHi )
                } else {
                    val intQueries = queriedFieldsEs.map( intField => makeIntRangeQuery( intField, intLo, intHi ) )
                    val intsBoolType = multiBoolType.getOrElse( BoolType.FILTER )

                    intsBoolType match {
                        case BoolType.MUST => makeBoolQuery( must = intQueries )
                        case BoolType.SHOULD => makeBoolQuery( should = intQueries )
                        case BoolType.FILTER => makeBoolQuery( filter = intQueries )
                        case BoolType.MUST_NOT => makeBoolQuery( mustNot = intQueries )
                    }
                }

            case CorpexTagQuery( _, tagId, tagTypes, tagTypesExact, typesBoolType, tagTypesQuery, tagValues, valuesBoolType, tagValuesQuery ) =>
                makeTagQuery(
                    tagId = TagIds.fromApi( tagId ),
                    tagTypes = tagTypes.map( _.map( tagType => AllTags.get( tagType, _.apiLabel ).get ) ),
                    tagTypesExact = tagTypesExact,
                    tagTypesQuery = tagTypesQuery,
                    typesBoolType = typesBoolType,
                    tagValues = tagValues,
                    tagValuesQuery = tagValuesQuery,
                    valuesBoolType = valuesBoolType )

            case CorpexFacetQuery( boolType, facetId, facetValues, valuesBoolType, scoreHi, scoreLo, scoreBoolType, facetValuesQuery ) =>

                makeFacetQuery(
                    facetId = FacetIds.fromApi( facetId ),
                    query = facetValuesQuery,
                    scoreLo = scoreLo,
                    scoreHi = scoreHi,
                    scoreBoolType = scoreBoolType,
                    facetValues = facetValues,
                    valuesBoolType = valuesBoolType )

            case CorpexBoolQuery( _, queries ) =>
                val mustQueries = queries
                  .filter( _.boolType == BoolType.MUST )
                  .map( query => corpexQueryToEsQuery( query ) )

                val shouldQueries = queries
                  .filter( _.boolType == BoolType.SHOULD )
                  .map( query => corpexQueryToEsQuery( query ) )

                val filterQueries = queries
                  .filter( _.boolType == BoolType.FILTER )
                  .map( query => corpexQueryToEsQuery( query ) )

                val mustNotQueries = queries
                  .filter( _.boolType == BoolType.MUST_NOT )
                  .map( query => corpexQueryToEsQuery( query ) )

                makeBoolQuery( mustQueries, shouldQueries, filterQueries, mustNotQueries )

            case _ =>
                throw new BadRequestBodyException( "query_type", Some( queryIn.queryType.toString ), "Invalid query type" )
        }
    }

    def corpexRequestToEsRequest( request : CorpexSearchRequest ) : EsSearchRequest = {

        val tenantFilter : List[ EsQuery ] = request.tenantId.map { tenantId =>
            EsQuery( termQuery = Some( Map( "tenants" -> TermQuery( tenantId ) ) ) )
        }.toList

        val aggs = request.aggs.map( _.map { tup : (String, CorpexAggQuery) =>
            val (aggLabel, corpexAggQuery) = tup
            (aggLabel, corpexAggQueryToEsAggQuery( corpexAggQuery, aggLabel ))
        } )

        val topQuery = if ( request.queries.isEmpty ) EsQuery() else {
            val mustQueries = request.queries.get
              .filter( _.boolType == BoolType.MUST )
              .map( query => corpexQueryToEsQuery( query ) )

            val shouldQueries = request.queries.get
              .filter( _.boolType == BoolType.SHOULD )
              .map( query => corpexQueryToEsQuery( query ) )

            // Add tenant filter to filters, if there is one
            val filterQueries = ( request.queries.get
              .filter( _.boolType == BoolType.FILTER )
              .map( query => corpexQueryToEsQuery( query ) ) ) ++ tenantFilter

            val mustNotQueries = request.queries.get
              .filter( _.boolType == BoolType.MUST_NOT )
              .map( query => corpexQueryToEsQuery( query ) )


            makeBoolQuery( mustQueries, shouldQueries, filterQueries, mustNotQueries )
        }

        val numResPerPage = request.pageSize.getOrElse( defaultPageSize )
        val from = request.page.map( _ * numResPerPage )

        val fieldsFlattened = request.fields.toList.flatten
        val allFields = ( if ( fieldsFlattened.isEmpty ) baseFields else fieldsFlattened )
          .map( field => DataIds.get( field, _.apiLabel ) )

        val sourceFields = allFields.collect { case Some( CdrDataId( _, _, Some( label ), _, _, _ ) ) => label }

        val storedFields = allFields.collect { case Some( CorpexDataId( _, _, Some( label ), _, _, _ ) ) => label }

        val sort = request.sort.map( _.map( key => {
            if (key.score.isDefined) EsSortKey( "_score", key.score )
            else {
                val dataId = DataIds.get( key.field.get, _.apiLabel ).get
                val field = if ( dataId.dataType == DataTypes.TERM ) dataId.cdrLabel.get + ".term"
                            else dataId.cdrLabel.get
                EsSortKey( field, key.direction )
            }
        } ) )

        EsSearchRequest( query = topQuery,
                         storedFields = if ( storedFields.isEmpty ) None else Some( storedFields ),
                         source = if ( sourceFields.isEmpty ) None else Some( sourceFields ),
                         from = from,
                         size = Some( numResPerPage ),
                         aggs = aggs,
                         sort = sort )
    }

    def convertSingleBucket( bucket : EsAggResult, corpexAggQuery : CorpexAggQuery, aggLabel : String  ) : Count = {
        val dataType = ParseQuery.getDataTypeFromAggQuery( corpexAggQuery )
        val key = bucket.key.getOrElse( throw new IllegalStateException( "Bucket without a key" ) )

        if ( bucket.aggs.contains( aggLabel + "_score" ) ) {
            ValueScore( bucket.aggs( aggLabel + "_score" ).value.getOrElse( throw new IllegalStateException( "Facet score agg did not return an average score value" ) ), key.string.getOrElse( throw new IllegalStateException( "Facet score agg returned a non-string key" ) ) )
        } else {
            val bucketCount = if ( bucket.aggs.isEmpty ) bucket.count.get else bucket.aggs.head._2.count.get

            dataType match {
                case DataTypes.TERM => ValueCount( bucketCount, key.string.getOrElse( throw new IllegalStateException( "Term agg returned a non-string key" ) ) )

                case DataTypes.DATE =>
                    bucket.keyString match {
                        case None =>
                            IntRangeCount( bucketCount,
                                           key.long.getOrElse( throw new IllegalStateException( "Date agg return a non-int key" ) ),
                                           ParseDate.getHiDateMs( key.long.get, corpexAggQuery.asInstanceOf[ CorpexFieldAggQuery ] ) )
                        case Some( strValue : String ) => DateRangeCount( bucketCount, strValue, ParseDate.getHiDateStr( strValue, corpexAggQuery.asInstanceOf[ CorpexFieldAggQuery ] ) )
                    }

                case DataTypes.INT =>
                    val dataId = ParseQuery.getDataIdFromAggQuery( corpexAggQuery )
                    key.get match {
                        case value : Long => IntRangeCount( bucketCount, value, value + dataId.defaultBucketSize.get.long.get )
                        case value : Double => FloatRangeCount( bucketCount, value, dataId.defaultBucketSize.get.get match {
                            case interval : Long => value + interval.toDouble
                            case interval : Double => value + interval
                        } )
                    }

            }
        }
    }

    @scala.annotation.tailrec
    final def esAggToCorpexAgg( esAgg : EsAggResult, corpexAggQuery : CorpexAggQuery, aggLabel : String ) : List[ Count ] = {
        if ( esAgg.buckets.isEmpty ) {
            esAggToCorpexAgg( esAgg.aggs.filter(_._1 != "meta" ).head._2, corpexAggQuery, aggLabel )
        }
        else if ( esAgg.buckets.get.isEmpty ) List()
        else if ( esAgg.buckets.get.length == 1 ) List( convertSingleBucket( esAgg.buckets.get.head, corpexAggQuery, aggLabel ) )
        else {
            val dataType = ParseQuery.getDataTypeFromAggQuery( corpexAggQuery )
            esAgg.buckets.get.zip( esAgg.buckets.get.tail ).map( ( tup : (EsAggResult, EsAggResult) ) => {
                val (aggLo, aggHi) = tup
                if ( aggLo.aggs.contains( aggLabel + "_score" ) ) {
                    ValueScore( aggLo.aggs( aggLabel + "_score" ).value.getOrElse( throw new IllegalStateException( "Facet score agg did not return an average score value" ) ), aggLo.key.flatMap( _.string ).getOrElse( throw new IllegalStateException( "Facet score agg returned a non-string key" ) ) )
                } else {
                    val bucketCount = if ( aggLo.aggs.isEmpty ) aggLo.count.get else aggLo.aggs.head._2.count.get
                    val key = aggLo.key.getOrElse( throw new IllegalStateException( "Bucket without a key" ) )

                    dataType match {
                        case DataTypes.TERM => ValueCount( bucketCount, key.string.getOrElse( throw new IllegalStateException( "Term agg returned a non-string key" ) ) )

                        case DataTypes.DATE =>
                            aggLo.keyString match {
                                case None => IntRangeCount( bucketCount, key.long.getOrElse( throw new IllegalStateException( "Date agg return a non-int key" ) ), aggHi.key.get.long
                                  .getOrElse( throw new IllegalStateException( "Date agg return a non-int key" ) ) )
                                case Some( strValue : String ) => DateRangeCount( bucketCount, strValue, aggHi.keyString.get )
                            }

                        case DataTypes.INT => key.get match {
                            case value : Long => IntRangeCount( bucketCount, value, aggHi.key.get.long.get )
                            case value : Double => FloatRangeCount( bucketCount, value, aggHi.key.get.double.get )
                        }

                    }
                }
            } ) :+ convertSingleBucket( esAgg.buckets.get.last, corpexAggQuery, aggLabel )
        }
    }

    def esResponseToCorpexResults( res : EsSearchResponse, corpexSearchRequest : CorpexSearchRequest ) : CorpexSearchResults = {
        val pageSize = corpexSearchRequest.pageSize
        val page = corpexSearchRequest.page
        val numPages = if ( pageSize.isDefined ) Some( Math.ceil( res.hits.total.total.toDouble / pageSize.get.toDouble ).toInt ) else None

        val allResults = res.hits.hits.map( ( singleRes : EsResponseResult ) => {
            val wordCount = singleRes.fields.collect {
                case p : Map[ String, List[ MultiValue ] ] if ( p.contains( DataIds.WORD_COUNT.cdrLabel.get ) ) =>
                    p( DataIds.WORD_COUNT.cdrLabel.get ).headOption.flatMap( _.long.map( _.toInt ) )
            }.flatten

            if ( singleRes.result == null || singleRes.result.cdr == null ) CorpexSingleResult( esScore = Some( singleRes.score ),
                                                                wordCount = wordCount,
                                                                cdr = null )
            else {
                val annotations = if ( singleRes.result.cdr.annotations == null ) List.empty else singleRes.result.cdr.annotations

                CorpexSingleResult( esScore = Some( singleRes.score ),
                                    wordCount = wordCount,
                                    cdr = singleRes.result.cdr.copy( annotations = annotations ) )
            }
        } )

        val aggs = res.aggregations.map( _.map { tup : (String, EsAggResult) =>
            val (aggLabel, esAgg) = tup

            (aggLabel, esAggToCorpexAgg( esAgg, corpexSearchRequest.aggs.get( aggLabel ), aggLabel ))
        } )

        CorpexSearchResults( numResults = res.hits.total.total,
                             exactNum = res.hits.total.exact,
                             page = page,
                             numPages = numPages,
                             pageSize = pageSize,
                             results = Some( allResults ),
                             aggregations = aggs )
    }

    def corpexRequestToEsCountRequest( request : CorpexSearchRequest ) : EsSearchRequest = {
        corpexRequestToEsRequest( request ).copy( source = None, from = None, size = None )
    }

}
