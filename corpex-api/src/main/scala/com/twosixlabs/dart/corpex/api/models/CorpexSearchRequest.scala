package com.twosixlabs.dart.corpex.api.models

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties, JsonInclude, JsonProperty, JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.twosixlabs.dart.corpex.api.configuration.annotations.{AllTags, FacetIds, TagIds, TagType}
import com.twosixlabs.dart.corpex.api.configuration.metadata.{DataId, DataIds, DataType, DataTypes}
import com.twosixlabs.dart.corpex.api.enums.{AggType, AggTypeType, BoolType, BoolTypeType, QueryType, QueryTypeType, SortType, SortTypeType}
import com.twosixlabs.dart.corpex.api.exceptions.{InvalidAggQueryException, InvalidRequestException, InvalidSearchQueryException}
import com.twosixlabs.dart.corpex.api.models.queries.{CorpexAggQuery, CorpexSearchQuery}
import com.twosixlabs.dart.corpex.api.tools.DateUtil

import scala.beans.BeanProperty
import scala.util.{Failure, Success, Try}

@JsonInclude( Include.NON_EMPTY )
case class CorpexSearchRequest( @BeanProperty @JsonProperty( "page" ) page : Option[ Int ] = None,
                                @BeanProperty @JsonProperty( "page_size" ) pageSize : Option[ Int ] = None,
                                @BeanProperty @JsonProperty( "fields" ) fields : Option[ List[ String ] ] = None,
                                @BeanProperty @JsonProperty( "sort" ) sort : Option[ List[ CorpexSortKey ] ] = None,
                                @BeanProperty @JsonProperty( "queries" ) queries : Option[ List[ CorpexSearchQuery ] ] = None,
                                @BeanProperty @JsonProperty( "aggregations" ) aggs : Option[ Map[ String, CorpexAggQuery ] ] = None,
                                @BeanProperty @JsonProperty( "tenant_id" ) tenantId : Option[ String ] = None )

@JsonInclude( Include.NON_EMPTY )
case class CorpexSortKey( @BeanProperty @JsonScalaEnumeration( classOf[ SortTypeType ] ) @JsonProperty( "score" ) score : Option[ SortType.SortType ] = None,
                          @BeanProperty @JsonProperty( "field" ) field : Option[ String ] = None,
                          @BeanProperty @JsonScalaEnumeration( classOf[ SortTypeType ] ) @JsonProperty( "direction" ) direction : Option[ SortType.SortType ] = None ) {

    if (score.isDefined && field.isDefined) throw new InvalidRequestException( "Sort key cannot have both `score` and `field` defined" )
    field.foreach( f => {
        val dataId = DataIds.get( f, _.apiLabel )
        if ( dataId.isEmpty ) throw new InvalidRequestException( s"Sort field ${f} is not recognized field id" )
        if ( dataId.get.cdrLabel.isEmpty ) throw new InvalidRequestException( s"Sort field ${f} is not a valid CDR field" )
        if ( dataId.get.dataType == DataTypes.TEXT ) throw new InvalidRequestException( s"Unable to sort on a text field (${f})" )
        if ( dataId.get.dataType == DataTypes.UNUSED ) throw new InvalidRequestException( s"Unable to sort on field: ${f}" )
    } )
}

