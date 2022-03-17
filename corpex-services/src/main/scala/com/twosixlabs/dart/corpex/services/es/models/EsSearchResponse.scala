package com.twosixlabs.dart.corpex.services.es.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonAnySetter, JsonIgnore, JsonIgnoreProperties, JsonInclude, JsonProperty}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode}
import com.twosixlabs.cdr4s.json.dart.DartCdrDocumentDto
import com.twosixlabs.dart.corpex.api.models.MultiValue

import scala.beans.BeanProperty
import scala.collection.mutable


@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class EsSearchResponse( @BeanProperty @JsonProperty( "_scroll_id" ) scrollId : Option[ String ] = None,
                             @BeanProperty @JsonProperty( "took" ) took : Int,
                             @BeanProperty @JsonProperty( "timed_out" ) timedOut : Boolean,
                             @BeanProperty @JsonProperty( "hits" ) hits : EsResponseHits,
                             @BeanProperty @JsonProperty( "aggregations" ) aggregations : Option[ Map[ String, EsAggResult ] ] = None )

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class EsAggResult( @BeanProperty @JsonProperty( "key" ) key : Option[ MultiValue ],
                        @BeanProperty @JsonProperty( "key_as_string" ) keyString : Option[ String ],
                        @BeanProperty @JsonProperty( "doc_count" ) count : Option[ Int ],
                        @BeanProperty @JsonProperty( "doc_count_error_upper_bound" ) docCountErrHi : Option[ Int ],
                        @BeanProperty @JsonProperty( "sum_other_doc_count" ) otherDocCount : Option[ Int ],
                        @BeanProperty @JsonProperty( "value" ) value : Option[ Double ],
                        @BeanProperty @JsonProperty( "buckets" ) buckets : Option[ List[ EsAggResult ] ] ) {

    @JsonIgnore
    val aggs : mutable.HashMap[ String, EsAggResult ] = scala.collection.mutable.HashMap[ String, EsAggResult ]()

    @JsonAnySetter
    def setAgg( key : String, value : EsAggResult ) : Unit = aggs( key ) = value

}

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class EsResponseHits( @BeanProperty @JsonDeserialize( using = classOf[ HitsDeserializer ] ) @JsonProperty( "total" ) total : EsResponseHitsTotal,
                           @BeanProperty @JsonProperty( "max_score" ) maxScore : Double,
                           @BeanProperty @JsonProperty( "hits" ) hits : List[ EsResponseResult ],
                         )

case class EsResponseHitsTotal( @BeanProperty total : Int, @BeanProperty exact : Boolean )

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class EsResponseResult( @BeanProperty @JsonProperty( "_index" ) index : String,
                             @BeanProperty @JsonProperty( "_type" ) hitType : String,
                             @BeanProperty @JsonProperty( "_id" ) id : String,
                             @BeanProperty @JsonProperty( "_score" ) score : Double,
                             @BeanProperty @JsonProperty( "fields" ) fields : Option[ Map[ String, List[ MultiValue ] ] ],
                             @BeanProperty @JsonProperty( "_source" ) result : DartEsDocument,
                           )

class HitsDeserializer extends JsonDeserializer[ EsResponseHitsTotal ] {
    override def deserialize( p : JsonParser, ctxt : DeserializationContext ) : EsResponseHitsTotal = {
        val node : JsonNode = p.getCodec.readTree( p )
        if ( node.isNumber ) EsResponseHitsTotal( node.asInt, exact = true )
        else {
            val total = node.get( "value" ).asInt
            val exact = if ( node.get( "relation" ).asText == "eq" ) true else false
            EsResponseHitsTotal( total, exact )
        }
    }
}
