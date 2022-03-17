package com.twosixlabs.dart.corpex.services.es.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
case class EsScrollRequest( @BeanProperty @JsonProperty( "scroll" ) scroll : Option[ String ] = None,
                            @BeanProperty @JsonProperty( "scroll_id" ) scrollId : String
                          )
