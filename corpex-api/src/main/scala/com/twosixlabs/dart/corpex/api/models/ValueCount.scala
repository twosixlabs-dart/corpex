package com.twosixlabs.dart.corpex.api.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonInclude, JsonProperty}

import scala.beans.BeanProperty

abstract class Count

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class ValueCount( @BeanProperty @JsonProperty( "num_docs" ) numDocs : Int,
                       @BeanProperty @JsonProperty( "value" ) value : String ) extends Count

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class ValueScore( @BeanProperty @JsonProperty( "score" ) score : Double,
                       @BeanProperty @JsonProperty( "value" ) value : String ) extends Count

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class IntRangeCount( @BeanProperty @JsonProperty( "num_docs" ) numDocs : Int,
                          @BeanProperty @JsonProperty( "lo" ) loValue : Long,
                          @BeanProperty @JsonProperty( "hi" ) hiValue : Long ) extends Count

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class DateRangeCount( @BeanProperty @JsonProperty( "num_docs" ) numDocs : Int,
                           @BeanProperty @JsonProperty( "lo" ) loValue : String,
                           @BeanProperty @JsonProperty( "hi" ) hiValue : String ) extends Count

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class FloatRangeCount( @BeanProperty @JsonProperty( "num_docs" ) numDocs : Int,
                            @BeanProperty @JsonProperty( "lo" ) loValue : Double,
                            @BeanProperty @JsonProperty( "hi" ) hiValue : Double ) extends Count
