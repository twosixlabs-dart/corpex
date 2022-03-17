package com.twosixlabs.dart.corpex.services.es.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonMappingException, JsonNode, JsonSerializer, SerializerProvider}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.twosixlabs.dart.corpex.api.enums.{SortType, SortTypeType}
import com.twosixlabs.dart.corpex.api.models.{MultiValue, MultiValueDeserializer}

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
case class EsSearchRequest( @BeanProperty @JsonProperty( "query" ) query : EsQuery,
                            @BeanProperty @JsonProperty( "aggs" ) aggs : Option[ Map[ String, EsAggQuery ] ] = None,
                            @BeanProperty @JsonProperty( "_source" ) source : Option[ List[ String ] ] = None,
                            @BeanProperty @JsonProperty( "stored_fields" ) storedFields : Option[ List[ String ] ] = None,
                            @BeanProperty @JsonProperty( "track_total_hits" ) totalHits : Option[ Boolean ] = None,
                            @BeanProperty @JsonProperty( "from" ) from : Option[ Int ] = None,
                            @BeanProperty @JsonProperty( "size" ) size : Option[ Int ] = None,
                            @BeanProperty @JsonProperty( "timeout" ) timeout : Option[ String ] = None,
                            @BeanProperty @JsonProperty( "sort" ) sort : Option[ List[ EsSortKey ] ] = None
                          )

@JsonInclude( Include.NON_EMPTY )
case class EsQuery( @BeanProperty @JsonProperty( "query_string" ) queryStringQuery : Option[ QueryStringQuery ] = None,
                    @BeanProperty @JsonProperty( "range" ) rangeQuery : Option[ Map[ String, RangeQuery ] ] = None,
                    @BeanProperty @JsonProperty( "match" ) matchQuery : Option[ Map[ String, MatchQuery ] ] = None,
                    @BeanProperty @JsonProperty( "term" ) termQuery : Option[ Map[ String, TermQuery ] ] = None,
                    @BeanProperty @JsonProperty( "bool" ) boolQuery : Option[ BoolQuery ] = None,
                    @BeanProperty @JsonProperty( "nested" ) nestedQuery : Option[ NestedQuery ] = None,
                  )

@JsonInclude( Include.NON_EMPTY )
case class QueryStringQuery( @BeanProperty @JsonProperty( "fields" ) fields : List[ String ],
                             @BeanProperty @JsonProperty( "query" ) query : String,
                             @BeanProperty @JsonProperty( "auto_generate_synonyms" ) genSynonyms : Option[ Boolean ] = None,
                             @BeanProperty @JsonProperty( "fuzzy_max_expansions" ) fuzzyMaxExp : Option[ Int ] = None )

abstract class RangeQuery( )

@JsonInclude( Include.NON_EMPTY )
case class LongRangeQuery( @BeanProperty @JsonProperty( "gte" ) gte : Option[ Long ] = None,
                           @BeanProperty @JsonProperty( "lte" ) lte : Option[ Long ] = None,
                           @BeanProperty @JsonProperty( "format" ) format : Option[ String ] = None ) extends RangeQuery

@JsonInclude( Include.NON_EMPTY )
case class DoubleRangeQuery( @BeanProperty @JsonProperty( "gte" ) gte : Option[ Double ] = None,
                             @BeanProperty @JsonProperty( "lte" ) lte : Option[ Double ] = None,
                             @BeanProperty @JsonProperty( "format" ) format : Option[ String ] = None ) extends RangeQuery

@JsonInclude( Include.NON_EMPTY )
case class MatchQuery( @BeanProperty @JsonProperty( "query" ) query : String,
                       @BeanProperty @JsonProperty( "operator" ) operator : Option[ String ] = None,
                     )

@JsonInclude( Include.NON_EMPTY )
case class TermQuery( @BeanProperty @JsonProperty( "value" ) value : String,
                      @BeanProperty @JsonProperty( "boost" ) boost : Option[ Double ] = None,
                    )

@JsonInclude( Include.NON_EMPTY )
case class BoolQuery( @BeanProperty @JsonProperty( "must" ) must : Option[ List[ EsQuery ] ] = None,
                      @BeanProperty @JsonProperty( "should" ) should : Option[ List[ EsQuery ] ] = None,
                      @BeanProperty @JsonProperty( "must_not" ) mustNot : Option[ List[ EsQuery ] ] = None,
                      @BeanProperty @JsonProperty( "filter" ) filter : Option[ List[ EsQuery ] ] = None,
                    )

@JsonInclude( Include.NON_EMPTY )
case class NestedQuery( @BeanProperty @JsonProperty( "path" ) path : String,
                        @BeanProperty @JsonProperty( "query" ) query : EsQuery
                      )

@JsonInclude( Include.NON_EMPTY )
case class EsAggQuery( @BeanProperty @JsonProperty( "nested" ) nested : Option[ AggQuery ] = None,
                       @BeanProperty @JsonProperty( "terms" ) terms : Option[ AggQuery ] = None,
                       @BeanProperty @JsonProperty( "date_histogram" ) date : Option[ AggQuery ] = None,
                       @BeanProperty @JsonProperty( "histogram" ) histogram : Option[ NumberAggQuery ] = None,
                       @BeanProperty @JsonProperty( "reverse_nested" ) reverseNested : Option[ AggQuery ] = None,
                       @BeanProperty @JsonProperty( "filter" ) filter : Option[ EsQuery ] = None,
                       @BeanProperty @JsonProperty( "avg" ) avg : Option[ AggQuery ] = None,
                       @BeanProperty @JsonProperty( "bucket_sort" ) bucketSort : Option[ AggBucketSort ] = None,
                       @BeanProperty @JsonProperty( "aggs" ) aggs : Option[ Map[ String, EsAggQuery ] ] = None,
                     )

@JsonInclude( Include.NON_EMPTY )
case class AggQuery( @BeanProperty @JsonProperty( "field" ) field : Option[ String ] = None,
                     @BeanProperty @JsonProperty( "path" ) path : Option[ String ] = None,
                     @BeanProperty @JsonProperty( "size" ) size : Option[ Int ] = None,
                     @BeanProperty @JsonProperty( "calendar_interval" ) calendarInt : Option[ String ] = None,
                     @BeanProperty @JsonProperty( "fixed_interval" ) fixedInt : Option[ String ] = None,
                     @BeanProperty @JsonProperty( "format" ) format : Option[ String ] = None,
                     @BeanProperty @JsonProperty( "order" ) order : Option[ EsSortKey ] = None,
                   )

abstract class NumberAggQuery

@JsonInclude( Include.NON_EMPTY )
case class LongAggQuery( @BeanProperty @JsonProperty( "field" ) field : Option[ String ] = None,
                         @BeanProperty @JsonProperty( "interval" ) interval : Option[ Long ] = None ) extends NumberAggQuery

@JsonInclude( Include.NON_EMPTY )
case class DoubleAggQuery( @BeanProperty @JsonProperty( "field" ) field : Option[ String ] = None,
                           @BeanProperty @JsonProperty( "interval" ) interval : Option[ Double ] = None ) extends NumberAggQuery

@JsonInclude( Include.NON_EMPTY )
case class AggBucketSort( @BeanProperty @JsonProperty( "size" ) size : Option[ Int ] = None,
                          @BeanProperty @JsonProperty( "sort" ) sort : Option[ List[ EsSortKey ] ] = None )

@JsonInclude( Include.NON_EMPTY )
@JsonSerialize( using = classOf[ EsSortKeySerializer ] )
case class EsSortKey( field : String, order : Option[ SortType.SortType ] )

class EsSortKeySerializer extends JsonSerializer[ EsSortKey ] {
    override def serialize( key : EsSortKey, gen : JsonGenerator, serializers : SerializerProvider ) : Unit = {
        if ( key.order.isEmpty ) gen.writeString( key.field )
        else {
            gen.writeStartObject()
            gen.writeStringField( key.field, key.order.get.toString )
            gen.writeEndObject()
        }
    }
}



