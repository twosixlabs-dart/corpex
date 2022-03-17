package com.twosixlabs.dart.corpex.api.models.profiles

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
case class FieldProfile( @BeanProperty @JsonProperty( "field_id" ) fieldId : String,
                         @BeanProperty @JsonProperty( "data_type" ) dataType : String,
                         @BeanProperty @JsonProperty( "cdr_label" ) cdrLabel : Option[ String ],
                         @BeanProperty @JsonProperty( "label" ) label : Option[ String ] = None,
                         @BeanProperty @JsonProperty( "description" ) description : Option[ String ] = None )
