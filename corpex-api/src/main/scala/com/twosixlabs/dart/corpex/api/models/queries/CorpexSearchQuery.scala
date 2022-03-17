package com.twosixlabs.dart.corpex.api.models.queries

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude, JsonProperty, JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.twosixlabs.dart.corpex.api.configuration.annotations.{AllTags, FacetIds, TagIds}
import com.twosixlabs.dart.corpex.api.configuration.metadata.{DataId, DataIds, DataTypes}
import com.twosixlabs.dart.corpex.api.enums.{BoolType, BoolTypeType, QueryType, QueryTypeType}
import com.twosixlabs.dart.corpex.api.exceptions.{InvalidRequestException, InvalidSearchQueryException}
import com.twosixlabs.dart.corpex.api.models.MultiValue
import com.twosixlabs.dart.corpex.api.tools.DateUtil

import java.time.OffsetDateTime
import scala.beans.BeanProperty

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "query_type" )
@JsonSubTypes( Array(
    new Type( value = classOf[ CorpexTextQuery ], name = "TEXT" ),
    new Type( value = classOf[ CorpexTermQuery ], name = "TERM" ),
    new Type( value = classOf[ CorpexIntegerQuery ], name = "INTEGER" ),
    new Type( value = classOf[ CorpexCdrDateQuery ], name = "CDR_DATE" ),
    new Type( value = classOf[ CorpexTagDateQuery ], name = "TAG_DATE" ),
    new Type( value = classOf[ CorpexTagQuery ], name = "TAG" ),
    new Type( value = classOf[ CorpexFacetQuery ], name = "FACET" ),
    new Type( value = classOf[ CorpexBoolQuery ], name = "BOOL" ) ) )
@JsonInclude( Include.NON_EMPTY )
abstract class CorpexSearchQuery {
    @BeanProperty @JsonScalaEnumeration( classOf[ QueryTypeType ] ) @JsonProperty( "query_type" ) val queryType : QueryType.QueryType
    @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "bool_type" ) val boolType : BoolType.BoolType

    if ( boolType == null ) throw new InvalidSearchQueryException( s"missing required field: bool_type" )

    def requireField( field : Any, fieldName : String ) : Unit =
        if ( field == null ) throw new InvalidSearchQueryException( s"${queryType.toString} query missing required field: ${fieldName}" )

    def validateQueriedFields( queriedFields : List[ String ] ) : Unit = queriedFields.foreach( field => {
        val dataId : DataId =
            DataIds.get( field, _.apiLabel ).getOrElse( throw new InvalidSearchQueryException( s"${field} is not a valid corpex field" ) )
        queryType match {
            case QueryType.TEXT => if ( dataId.dataType != DataTypes.TEXT && dataId.dataType != DataTypes.TERM )
                throw new InvalidSearchQueryException( s"${field} is not a valid TEXT field" )
            case QueryType.TERM => if ( dataId.dataType != DataTypes.TERM ) throw new InvalidSearchQueryException( s"${field} is not a valid TERM field" )
            case QueryType.INTEGER => if ( dataId.dataType != DataTypes.INT ) throw new InvalidSearchQueryException( s"${field} is not a valid INTEGER field" )
            case QueryType.CDR_DATE => if ( dataId.dataType != DataTypes.DATE ) throw new InvalidSearchQueryException( s"${field} is not a valid DATE field" )
        }
    } )
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexTextQuery( @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "bool_type" ) override val boolType : BoolType.BoolType,
                            @BeanProperty @JsonProperty( "queried_fields" ) queriedFields : List[ String ],
                            @BeanProperty @JsonProperty( "query_string" ) queryString : String )
  extends CorpexSearchQuery {
    @BeanProperty @JsonIgnore override val queryType = QueryType.TEXT

    requireField( queriedFields, "queried_fields" )
    validateQueriedFields( queriedFields )
    requireField( queryString, "query_string " )
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexTermQuery( @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "bool_type" ) override val boolType : BoolType.BoolType,
                            @BeanProperty @JsonProperty( "queried_field" ) queriedField : String,
                            @BeanProperty @JsonProperty( "term_values" ) termValues : List[ String ],
                            @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "values_bool_type" ) valuesBoolType : Option[ BoolType.BoolType ]
                          )
  extends CorpexSearchQuery {
    @BeanProperty @JsonIgnore override val queryType = QueryType.TERM

    requireField( queriedField, "queried_field" )
    validateQueriedFields( List( queriedField ) )
    requireField( termValues, "term_values" )
    valuesBoolType.foreach( bt => {
        if (bt == BoolType.FILTER || bt == BoolType.MUST)
            throw new InvalidSearchQueryException( s"TERM query values_bool_type field cannot have value ${bt.toString}: supported values are SHOULD or MUST_NOT" )
    } )
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexIntegerQuery( @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "bool_type" ) override val boolType : BoolType.BoolType,
                               @BeanProperty @JsonProperty( "queried_fields" ) queriedFields : List[ String ],
                               @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "multi_bool_type" ) multiBoolType : Option[ BoolType.BoolType ] = None,
                               @BeanProperty @JsonDeserialize( contentAs = classOf[ java.lang.Long ] ) @JsonProperty( "int_hi" ) intHi : Option[ Long ] = None,
                               @BeanProperty @JsonDeserialize( contentAs = classOf[ java.lang.Long ] ) @JsonProperty( "int_lo" ) intLo : Option[ Long ] = None )
  extends CorpexSearchQuery {
    @BeanProperty @JsonIgnore override val queryType = QueryType.INTEGER

    requireField( queriedFields, "queried_fields" )
    validateQueriedFields( queriedFields )
    if ( intLo.isEmpty && intHi.isEmpty ) throw new InvalidSearchQueryException( "INTEGER query must include at least one of the fields: int_lo, int_hi" )
}

@JsonInclude( Include.NON_EMPTY )
abstract class CorpexDateQuery extends CorpexSearchQuery {
    @BeanProperty @JsonProperty( "date_hi" ) val dateHi : MultiValue
    @BeanProperty @JsonProperty( "date_lo" ) val dateLo : MultiValue

    def validateDates : Unit = if ( dateLo.isEmpty && dateHi.isEmpty ) throw new InvalidSearchQueryException( s"${queryType.toString} query must include at least one of the " +
                                                                                                              s"fields: date_lo, date_hi" )

    @JsonIgnore val dateLoDate : Option[ OffsetDateTime ] = if ( dateLo.isEmpty ) None else Some( DateUtil.getDateFromMultiValue( dateLo, lo = true ) )
    @JsonIgnore val dateHiDate : Option[ OffsetDateTime ] = if ( dateHi.isEmpty ) None else Some( DateUtil.getDateFromMultiValue( dateHi, lo = false ) )
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexCdrDateQuery( @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "bool_type" ) override val boolType : BoolType.BoolType,
                               @BeanProperty @JsonProperty( "queried_fields" ) queriedFields : List[ String ],
                               @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "multi_bool_type" ) multiBoolType : Option[ BoolType.BoolType ] = None,
                               @BeanProperty @JsonProperty( "date_hi" ) override val dateHi : MultiValue = MultiValue(),
                               @BeanProperty @JsonProperty( "date_lo" ) override val dateLo : MultiValue = MultiValue() )
  extends CorpexDateQuery {
    @BeanProperty @JsonIgnore override val queryType = QueryType.CDR_DATE

    requireField( queriedFields, "queried_fields" )
    validateQueriedFields( queriedFields )
    validateDates
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexTagDateQuery( @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "bool_type" ) override val boolType : BoolType.BoolType,
                               @BeanProperty @JsonProperty( "date_hi" ) override val dateHi : MultiValue = MultiValue(),
                               @BeanProperty @JsonProperty( "date_lo" ) override val dateLo : MultiValue = MultiValue() )
  extends CorpexDateQuery {
    @BeanProperty @JsonIgnore override val queryType = QueryType.TAG_DATE

    validateDates
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexTagQuery( @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "bool_type" ) override val boolType : BoolType.BoolType,
                           @BeanProperty @JsonProperty( "tag_id" ) tagId : String,
                           @BeanProperty @JsonProperty( "tag_types" ) tagTypes : Option[ List[ String ] ] = None,
                           @BeanProperty @JsonProperty( "tag_types_exact" ) tagTypesExact : Option[ List[ String ] ] = None,
                           @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "types_bool_type" ) typesBoolType : Option[ BoolType.BoolType ] = None,
                           @BeanProperty @JsonProperty( "tag_types_query" ) tagTypesQuery : Option[ String ] = None,
                           @BeanProperty @JsonProperty( "tag_values" ) tagValues : Option[ List[ String ] ] = None,
                           @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "values_bool_type" ) valuesBoolType : Option[ BoolType.BoolType ] = None,
                           @BeanProperty @JsonProperty( "tag_values_query" ) tagValuesQuery : Option[ String ] = None )
  extends CorpexSearchQuery {
    @BeanProperty @JsonIgnore override val queryType = QueryType.TAG

    requireField( tagId, "tag_id" )
    if ( !TagIds.check( tagId, _.apiLabel ) ) throw new InvalidSearchQueryException( s"${tagId} is not a valid tag id" )
    tagTypes.foreach( _.foreach( tagType => if ( !AllTags.check( tagType, _.apiLabel ) ) throw new InvalidSearchQueryException( s"${tagType} is not a valid tag type" ) ) )
    if ( tagTypes.isDefined && tagTypesExact.isDefined ) throw new InvalidSearchQueryException( "tag_types and tag_types_exact cannot both be defined" )
    if ( tagTypes.isDefined && tagTypesQuery.isDefined ) throw new InvalidSearchQueryException( "tag_types and tag_types_query cannot both be defined" )
    if ( tagTypesExact.isDefined && tagTypesQuery.isDefined ) throw new InvalidSearchQueryException( "tag_types_exact and tag_types_query cannot both be defined" )
    if ( tagValues.isDefined && tagValuesQuery.isDefined ) throw new InvalidSearchQueryException( "tag_values and tag_values_query cannot both be defined" )
    if ( valuesBoolType.isDefined && tagValues.isEmpty ) throw new InvalidSearchQueryException( "values_bool_type is defined without tag_values" )
    if ( typesBoolType.isDefined && tagTypes.isEmpty && tagTypes.isEmpty ) throw new InvalidSearchQueryException( "types_bool_type is defined without tag_types or " +
                                                                                                                  "tag_types_exact" )
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexFacetQuery( @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "bool_type" ) override val boolType : BoolType.BoolType,
                             @BeanProperty @JsonProperty( "facet_id" ) facetId : String,
                             @BeanProperty @JsonProperty( "facet_values" ) facetValues : Option[ List[ String ] ] = None,
                             @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "values_bool_type" ) valuesBoolType : Option[ BoolType.BoolType ] = None,
                             @BeanProperty @JsonDeserialize( contentAs = classOf[ java.lang.Double ] ) @JsonProperty( "score_hi" ) scoreHi : Option[ Double ] = None,
                             @BeanProperty @JsonDeserialize( contentAs = classOf[ java.lang.Double ] ) @JsonProperty( "score_lo" ) scoreLo : Option[ Double ] = None,
                             @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "score_bool_type" ) scoreBoolType : Option[ BoolType.BoolType ] = None,
                             @BeanProperty @JsonProperty( "facet_values_query" ) facetValuesQuery : Option[ String ] = None )
  extends CorpexSearchQuery {
    @BeanProperty @JsonIgnore override val queryType = QueryType.FACET

    requireField( facetId, "facet_id" )
    if ( !FacetIds.check( facetId, _.apiLabel ) ) throw new InvalidSearchQueryException( s"${facetId} is not a valid facet id" )
    if ( facetValues.isDefined && facetValuesQuery.isDefined ) throw new InvalidSearchQueryException( "facet_values and facet_values_query cannot both be defined" )

    scoreLo.foreach( ( score : Double ) => if ( score < 0.0 || score > 1.0 ) throw new InvalidRequestException( s"score_lo (${score}) is not between 0 and 1" ) )
    scoreHi.foreach( ( score : Double ) => if ( score < 0.0 || score > 1.0 ) throw new InvalidRequestException( s"score_hi (${score}) is not between 0 and 1" ) )
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexBoolQuery( @BeanProperty @JsonScalaEnumeration( classOf[ BoolTypeType ] ) @JsonProperty( "bool_type" ) override val boolType : BoolType.BoolType,
                            @BeanProperty @JsonProperty( "queries" ) queries : List[ CorpexSearchQuery ] )
  extends CorpexSearchQuery {
    @BeanProperty @JsonIgnore override val queryType = QueryType.BOOL

    requireField( queries, "queries" )
}