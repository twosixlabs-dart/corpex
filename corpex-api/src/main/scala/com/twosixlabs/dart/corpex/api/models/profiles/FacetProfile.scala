package com.twosixlabs.dart.corpex.api.models.profiles

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}

import scala.beans.BeanProperty

@JsonInclude( Include.NON_EMPTY )
case class FacetProfile( @BeanProperty @JsonProperty( "facet_id" ) facetId : String,
                         @BeanProperty @JsonProperty( "has_score" ) hasScore : Boolean,
                         @BeanProperty @JsonProperty( "cdr_label" ) cdrLabel : String,
                         @BeanProperty @JsonProperty( "label" ) label : Option[ String ] = None,
                         @BeanProperty @JsonProperty( "description" ) description : Option[ String ] = None )
