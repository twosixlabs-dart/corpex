package com.twosixlabs.dart.corpex.services.aggregation

import com.twosixlabs.dart.corpex.services.aggregation.models.AggregationQuery
import com.twosixlabs.dart.exceptions.BadQueryParameterException

case class PredefQuery( )

trait QueryService {

    def getQueries : Map[ String, AggregationQuery ]

    def getQuery( id : String ) : Option[ AggregationQuery ]

    def putQuery( id : String, query : AggregationQuery ) : AggregationQuery

    def patchQuery( id : String, query : AggregationQuery ) : AggregationQuery

    def delQuery( id : String ) : Unit

    def parseParams( queryName : Option[ String ],
                     tagId : Option[ String ],
                     tagType : Option[ String ],
                     facetId : Option[ String ],
                     fieldId : Option[ String ],
                     minResults : Option[ String ],
                     maxResults : Option[ String ] ) : AggregationQuery = {

        queryName.map( id => {
            val predefQuery = getQuery( id )
            if (predefQuery.isDefined) predefQuery.get
            else throw new BadQueryParameterException( "queryName", Some( id ), s"Predefined query ${id} does not exist" )
        } ) match {
            case Some( predefQuery : AggregationQuery ) => predefQuery.update( tagId, tagType, facetId, fieldId, minResults, maxResults )
            case None => AggregationQuery.fromParams( tagId, tagType, facetId, fieldId, minResults, maxResults )
        }
    }

}

class ParameterizedQueryService( props : Map[ String, String ] ) extends QueryService {

    private val paramNames = Set( "tagId", "tagType", "facetId", "fieldId", "minResults", "maxResults" )
    val queries : Map[ String, AggregationQuery ] = getQueryMapFromProperties( props )

    override def getQueries : Map[ String, AggregationQuery ] = queries

    override def getQuery( id : String ) : Option[ AggregationQuery ] = {
        queries.get( id )
    }

    override def putQuery( id : String, query : AggregationQuery ) : AggregationQuery = ???

    override def patchQuery( id : String, query : AggregationQuery ) : AggregationQuery = ???

    override def delQuery( id : String ) : Unit = ???

    def getQueryMapFromProperties( props : Map[ String, String ] ) : Map[ String, AggregationQuery ] = {

        props.withFilter( _._1.startsWith( "predefined.query." ) ).map { tup =>
            val (key, value) = tup
            val id = key.stripPrefix( "predefined.query." ).trim
            val params = value.split( "," ).map( pairString => {
                val pairList = pairString.split( ':' ).map( _.trim )
                if ( pairList.length != 2 ) throw new Exception( s"Invalid query parameter: ${pairString}" )

                if ( !paramNames.contains( pairList( 0 ) ) ) throw new Exception( s"Invalid query parameter; ${pairList( 0 )}" )

                (pairList( 0 ), pairList( 1 ))
            } ).toMap

            (id, AggregationQuery.fromParams( params.get( "tagId" ),
                                              params.get( "tagType" ),
                                              params.get( "facetId" ),
                                              params.get( "fieldId" ),
                                              params.get( "minResults" ),
                                              params.get( "maxResults" ) ) )
        }
    }

}
