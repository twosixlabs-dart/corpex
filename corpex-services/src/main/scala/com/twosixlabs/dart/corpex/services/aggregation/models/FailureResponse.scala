package com.twosixlabs.dart.corpex.services.aggregation.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonInclude, JsonProperty}

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class FailureResponse( @BeanProperty @JsonProperty( "status" ) status : Int,
                            @BeanProperty @JsonProperty( "error_message" ) message : String )
