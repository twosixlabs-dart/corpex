package com.twosixlabs.dart.corpex.services.es.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonInclude, JsonProperty, JsonUnwrapped}
import com.twosixlabs.cdr4s.json.dart.DartCdrDocumentDto

import scala.beans.BeanProperty


@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class EsDocumentResponse( @BeanProperty @JsonProperty( "_source" ) dartDoc : DartEsDocument )

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
class DartEsDocument( cdrIn : DartCdrDocumentDto,
                      privateFieldsIn : DartPrivateFields ) {
    @BeanProperty
    @JsonUnwrapped
    val cdr : DartCdrDocumentDto = cdrIn


    @BeanProperty
    @JsonUnwrapped
    val privateFields : DartPrivateFields = privateFieldsIn
}

object DartEsDocument {
    def apply( cdrIn : DartCdrDocumentDto,
               privateFieldsIn : DartPrivateFields ) : DartEsDocument = new DartEsDocument( cdrIn, privateFieldsIn )
}

@JsonInclude( Include.NON_EMPTY )
@JsonIgnoreProperties( ignoreUnknown = true )
case class DartPrivateFields( @BeanProperty @JsonProperty( "tenants" ) tenants : Option[ List[ String ] ] = None )
