package com.twosixlabs.dart.corpex.api.models.queries

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude, JsonProperty, JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.twosixlabs.dart.corpex.api.configuration.annotations.{AllTags, FacetIds, TagIds}
import com.twosixlabs.dart.corpex.api.configuration.metadata.{DataIds, DataTypes}
import com.twosixlabs.dart.corpex.api.enums.{AggType, AggTypeType}
import com.twosixlabs.dart.corpex.api.exceptions.{InvalidAggQueryException, InvalidRequestException, InvalidSearchQueryException}
import com.twosixlabs.dart.corpex.api.models.MultiValue
import com.twosixlabs.dart.corpex.api.tools.DateUtil

import java.time.OffsetDateTime
import scala.beans.BeanProperty
import scala.util.{Failure, Success, Try}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "agg_type" )
@JsonSubTypes( Array(
    new Type( value = classOf[ CorpexFieldAggQuery ], name = "FIELD" ),
    new Type( value = classOf[ CorpexTagTypesAggQuery ], name = "TAG_TYPES" ),
    new Type( value = classOf[ CorpexTagValuesAggQuery ], name = "TAG_VALUES" ),
    new Type( value = classOf[ CorpexFacetAggQuery ], name = "FACET" ),
    new Type( value = classOf[ CorpexFacetScoreAggQuery ], name = "FACET_CONFIDENCE" ) ) )
@JsonInclude( Include.NON_EMPTY )
abstract class CorpexAggQuery {
    @BeanProperty
    @JsonScalaEnumeration( classOf[ AggTypeType ] )
    @JsonProperty( "agg_type" ) val aggType : AggType.AggType
    @BeanProperty
    @JsonProperty( "size" ) val size : Option[ Int ]

    def requireField( field : Any, fieldName : String ) : Unit =
        if ( field == null ) throw new InvalidSearchQueryException( s"${aggType.toString} aggregation query missing required field: ${fieldName}" )
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexFieldAggQuery( @BeanProperty @JsonProperty( "queried_field" ) queriedField : String,
                                @BeanProperty @JsonProperty( "bucket_size" ) bucketSize : MultiValue = MultiValue(),
                                @BeanProperty @JsonProperty( "lo" ) lo : MultiValue = MultiValue(),
                                @BeanProperty @JsonProperty( "hi" ) hi : MultiValue = MultiValue(),
                                @BeanProperty @JsonProperty( "values_query" ) valuesQuery : Option[ String ] = None,
                                @BeanProperty @JsonIgnore override val size : Option[ Int ] = None )
  extends CorpexAggQuery {
    @BeanProperty @JsonScalaEnumeration( classOf[ AggTypeType ] ) @JsonProperty( "agg_type" ) override val aggType : AggType.AggType = AggType.FIELD

    requireField( queriedField, "queried_field" )
    val dataId = DataIds.get( queriedField, _.apiLabel )
      .getOrElse( throw new InvalidAggQueryException( s"${queriedField} is not a valid corpex field" ) )
    if ( Set( DataTypes.UNUSED, DataTypes.TEXT ).contains( dataId.dataType ) ) throw new InvalidAggQueryException( s"${queriedField} is not a field that can be aggregated" )
    if ( bucketSize.isDefined || lo.isDefined || hi.isDefined ) {
        if ( !Set( DataTypes.DATE, DataTypes.INT ).contains( dataId.dataType ) )
            throw new InvalidAggQueryException( s"${queriedField} does not support range query" )
    }

    if ( valuesQuery.isDefined && dataId.dataType != DataTypes.TERM ) throw new InvalidAggQueryException( s"values_query (${valuesQuery.get}) cannot be defined for a field of " +
                                                                                                          s"type ${dataId.dataType.dataType.toUpperCase}" )

    val dateLo : Option[ OffsetDateTime ] = if ( lo.isDefined && DataIds.get( queriedField, _.apiLabel ).get.dataType == DataTypes.DATE ) {
        Try( Some( DateUtil.getDateFromMultiValue( lo, lo = true ) ) ) match {
            case Success( res ) => res
            case Failure( e : InvalidRequestException ) =>
                throw new InvalidAggQueryException( e.getMessage.stripPrefix( "invalid search or aggregation query: " ) )
            case Failure( t ) => throw t
        }
    } else None

    val dateHi : Option[ OffsetDateTime ] = if ( hi.isDefined && DataIds.get( queriedField, _.apiLabel ).get.dataType == DataTypes.DATE ) {
        Try( Some( DateUtil.getDateFromMultiValue( hi, lo = false ) ) ) match {
            case Success( res ) => res
            case Failure( e : InvalidRequestException ) =>
                throw new InvalidAggQueryException( e.getMessage.stripPrefix( "invalid search or aggregation query: " ) )
        }
    } else None
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexTagTypesAggQuery( @BeanProperty @JsonProperty( "tag_id" ) tagId : String,
                                   @BeanProperty @JsonProperty( "tag_types_query" ) tagTypesQuery : Option[ String ] = None,
                                   @BeanProperty @JsonIgnore override val size : Option[ Int ] = None )
  extends CorpexAggQuery {
    @BeanProperty @JsonScalaEnumeration( classOf[ AggTypeType ] ) @JsonProperty( "agg_type" )
    override val aggType : AggType.AggType = AggType.TAG_TYPES

    requireField( tagId, "tag_id" )
    if ( !TagIds.check( tagId, _.apiLabel ) ) throw new InvalidAggQueryException( s"${tagId} is not a valid tag id" )
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexTagValuesAggQuery( @BeanProperty @JsonProperty( "tag_id" ) tagId : String,
                                    @BeanProperty @JsonProperty( "tag_types" ) tagTypes : Option[ List[ String ] ] = None,
                                    @BeanProperty @JsonProperty( "tag_types_exact" ) tagTypesExact : Option[ List[ String ] ] = None,
                                    @BeanProperty @JsonProperty( "tag_types_query" ) tagTypesQuery : Option[ String ] = None,
                                    @BeanProperty @JsonProperty( "tag_values_query" ) tagValuesQuery : Option[ String ] = None,
                                    @BeanProperty @JsonIgnore override val size : Option[ Int ] = None )
  extends CorpexAggQuery {
    @BeanProperty @JsonScalaEnumeration( classOf[ AggTypeType ] ) @JsonProperty( "agg_type" )
    override val aggType : AggType.AggType = AggType.TAG_VALUES

    requireField( tagId, "tag_id" )
    if ( !TagIds.check( tagId, _.apiLabel ) ) throw new InvalidAggQueryException( s"${tagId} is not a valid tag id" )
    tagTypes.foreach( _.foreach( tagType => {
        if ( !AllTags.check( tagType, _.apiLabel ) ) throw new InvalidAggQueryException( s"${tagType} is not a valid tag type" )
    } ) )
    if ( tagTypes.isDefined && tagTypesExact.isDefined ) throw new InvalidAggQueryException( "tag_types and tag_types_exact cannot both be defined" )
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexFacetAggQuery( @BeanProperty @JsonProperty( "facet_id" ) facetId : String,
                                @BeanProperty @JsonProperty( "facet_values_query" ) facetValuesQuery : Option[ String ] = None,
                                @BeanProperty @JsonDeserialize( contentAs = classOf[ java.lang.Double ] ) @JsonProperty( "score_lo" ) scoreLo : Option[ Double ] = None,
                                @BeanProperty @JsonDeserialize( contentAs = classOf[ java.lang.Double ] ) @JsonProperty( "score_hi" ) scoreHi : Option[ Double ] = None,
                                @BeanProperty @JsonIgnore override val size : Option[ Int ] = None )
  extends CorpexAggQuery {
    @BeanProperty @JsonScalaEnumeration( classOf[ AggTypeType ] ) @JsonProperty( "agg_type" )
    override val aggType : AggType.AggType = AggType.FACET

    requireField( facetId, "facet_id" )
    if ( !FacetIds.check( facetId, _.apiLabel ) ) throw new InvalidAggQueryException( s"${facetId} is not a valid facet id" )

    scoreLo.foreach( ( score : Double ) => if ( score < 0.0 || score > 1.0 ) throw new InvalidAggQueryException( s"score_lo (${score}) is not between 0 and 1" ) )
    scoreHi.foreach( ( score : Double ) => if ( score < 0.0 || score > 1.0 ) throw new InvalidAggQueryException( s"score_hi (${score}) is not between 0 and 1" ) )
}

@JsonInclude( Include.NON_EMPTY )
case class CorpexFacetScoreAggQuery( @BeanProperty @JsonProperty( "facet_id" ) facetId : String,
                                          @BeanProperty @JsonProperty( "facet_values_query" ) facetValuesQuery : Option[ String ] = None,
                                          @BeanProperty @JsonIgnore override val size : Option[ Int ] = None )
  extends CorpexAggQuery {
    @BeanProperty
    @JsonScalaEnumeration( classOf[ AggTypeType ] )
    @JsonProperty( "agg_type" )
    override val aggType : AggType.AggType = AggType.FACET_CONFIDENCE

    requireField( facetId, "facet_id" )
    if ( !FacetIds.check( facetId, _.apiLabel ) ) throw new InvalidAggQueryException( s"${facetId} is not a valid facet id" )

}
