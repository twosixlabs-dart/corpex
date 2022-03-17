package com.twosixlabs.dart.corpex.api.models.profiles

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
case class TagProfile( @BeanProperty @JsonProperty( "tag_id" ) tagId : String,
                       @BeanProperty @JsonProperty( "cdr_label" ) cdrLabel : String,
                       @BeanProperty @JsonProperty( "label" ) label : Option[ String ] = None,
                       @BeanProperty @JsonProperty( "description" ) description : Option[ String ] = None )
