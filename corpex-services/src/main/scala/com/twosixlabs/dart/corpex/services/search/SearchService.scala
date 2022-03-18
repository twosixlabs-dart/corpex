package com.twosixlabs.dart.corpex.services.search

import com.twosixlabs.cdr4s.json.dart.DartCdrDocumentDto
import com.twosixlabs.dart.corpex.api.models.{ CorpexSearchRequest, CorpexSearchResults }
import com.typesafe.config.Config

import scala.concurrent.Future
import scala.util.Try

object SearchService {

    trait Dependencies {
        val defaultPageSize : Int
        val baseFields : List[ String ]
        val defaultTextField : String
    }

    trait DI extends Dependencies {
        def buildSearchService : SearchService
        lazy val searchService : SearchService = buildSearchService
    }

    def deps(
        defaultPageSize : Int,
        baseFields : List[ String ],
        defaultTextField : String,
    ) : Dependencies = {
        val dps = defaultPageSize; val bf = baseFields; val dtf = defaultTextField
        new Dependencies {
            override val defaultPageSize : Int = dps
            override val baseFields : List[String ] = bf
            override val defaultTextField : String = dtf
        }
    }

    def deps( config : Config ) : Dependencies = deps(
        config.getInt( "corpex.default.page.size" ),
        config.getString( "corpex.default.base.fields" ).split( "," ).map( _.trim ).toList,
        Try( config.getString( "corpex.default.text.field" ) ).getOrElse( "cdr.extracted_text" )
    )
}

trait SearchService extends SearchService.Dependencies {
    def search( corpexRequest : CorpexSearchRequest ) : CorpexSearchResults

    def count( corpexRequest : CorpexSearchRequest ) : CorpexSearchResults

    def getDocument( id : String, fieldsIncl : Option[ String ] = None, fieldsExcl : Option[ String ] = None ) : DartCdrDocumentDto

    def getCroppedCdr( dto : DartCdrDocumentDto, fields : Set[ String ] ) : DartCdrDocumentDto

    def shave( searchRequest : CorpexSearchRequest, take : Int ) : List[ String ]

    def serviceCheck : Future[ (Boolean, Option[ String ]) ]
}
