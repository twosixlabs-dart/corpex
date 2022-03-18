package com.twosixlabs.dart.corpex.services.aggregation.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonInclude, JsonProperty}

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class ValueCount( @BeanProperty @JsonProperty( "count" ) count : Int,
                       @BeanProperty @JsonProperty( "value" ) value : String )
