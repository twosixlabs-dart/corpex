package com.twosixlabs.dart.corpex.services.search.es.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonInclude, JsonProperty}

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class EsCountResponse( @BeanProperty @JsonProperty( "count" ) count : Option[ Int ] = None )
