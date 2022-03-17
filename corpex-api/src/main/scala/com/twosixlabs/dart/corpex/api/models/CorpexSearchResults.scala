package com.twosixlabs.dart.corpex.api.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}
import com.twosixlabs.cdr4s.json.dart.DartCdrDocumentDto

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
case class CorpexSearchResults( @BeanProperty @JsonProperty( "num_results" ) numResults : Int,
                                @BeanProperty @JsonProperty( "exact_num" ) exactNum : Boolean,
                                @BeanProperty @JsonProperty( "page" ) page : Option[ Int ] = None,
                                @BeanProperty @JsonProperty( "num_pages" ) numPages : Option[ Int ] = None,
                                @BeanProperty @JsonProperty( "page_size" ) pageSize : Option[ Int ] = None,
                                @BeanProperty @JsonProperty( "results" ) results : Option[ List[ CorpexSingleResult ] ] = None,
                                @BeanProperty @JsonProperty( "aggregations" ) aggregations : Option[ Map[ String, List[ Count ] ] ] = None )

@JsonInclude( Include.NON_EMPTY )
case class CorpexSingleResult( @BeanProperty @JsonProperty( "es_score" ) esScore : Option[ Double ] = None,
                               @BeanProperty @JsonProperty( "word_count" ) wordCount : Option[ Int ] = None,
                               @BeanProperty @JsonProperty( "cdr" ) cdr : DartCdrDocumentDto )

