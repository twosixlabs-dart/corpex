package com.twosixlabs.dart.corpex.api.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonInclude, JsonProperty}

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class CorpexFailureResponse( @BeanProperty @JsonProperty( "status" ) status : Int,
                                  @BeanProperty @JsonProperty( "error" ) error : CorpexErrorObject )

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class CorpexErrorObject( @BeanProperty @JsonProperty( "message" ) message : String,
                              @BeanProperty @JsonProperty( "exception" ) exception : Option[ String ] = None,
                              @BeanProperty @JsonProperty( "stack_trace" ) stackTrace : Option[ String ] = None,
                            )
