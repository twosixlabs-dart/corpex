package com.twosixlabs.dart.corpex.controller

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.corpex.services.aggregation.exceptions.QueryValidationException
import com.twosixlabs.dart.corpex.services.aggregation.models.AggregationQuery
import com.twosixlabs.dart.corpex.services.aggregation.{ CdrService, QueryService }
import com.twosixlabs.dart.exceptions.BadQueryParameterException
import org.scalatra.Ok
import org.slf4j.{ Logger, LoggerFactory }

import scala.util.{ Failure, Success, Try }

class CdrAggregationController( queryService : QueryService,
                                cdrService : CdrService,
                                basedDeps : CorpexBaseController.Dependencies )
  extends CorpexBaseController( basedDeps ) with SecureDartController {

    override val serviceName : String = "cdr-aggregation"

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    get( "/:docId/aggregate" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        val docId = params( "docId" )
        val date = params.get( "date" )
        val aggregationQuery = queryService.parseParams( queryName = params.get( "queryName" ),
                                                         tagId = params.get( "tagId" ),
                                                         tagType = params.get( "tagType" ),
                                                         facetId = params.get( "facetId" ),
                                                         fieldId = params.get( "fieldId" ),
                                                         minResults = params.get( "minResults" ),
                                                         maxResults = params.get( "maxResults" ) )

        Try( Ok( cdrService.getAggregation( docId, aggregationQuery, date ) ) ) recoverWith {
            case e : QueryValidationException => Failure( new BadQueryParameterException( e.getMessage ) )
            case e : Throwable => Failure( e )
        }
    } ) )

    get( "/:docId/aggregate-all" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        val minRes = Try( params.get( "minResults" ).map( _.toInt ) ) match {
            case Success( m : Option[ Int ] ) => m;
            case Failure( _ ) => throw new BadQueryParameterException( "minResults", params.get( "maxResults" ), "Integer" )
        }

        val maxRes = Try( params.get("maxResults").map( _.toInt ) ) match {
            case Success( m : Option[ Int ] ) => m;
            case Failure( _ ) => throw new BadQueryParameterException( "maxResults", params.get( "maxResults" ), "Integer" )
        }

        val docId = params( "docId" )
        val date = params.get( "date" )

        val queries = queryService.getQueries.map( ( tup : (String, AggregationQuery) ) => {
            val (queryName, query) = tup
            val minQuery = if ( minRes.isDefined ) query.copy( minResults = minRes ) else query
            val minAndMaxQuery = if ( maxRes.isDefined ) query.copy( maxResults = maxRes ) else minQuery
            (queryName, minAndMaxQuery)
        } )

        val results = queries.map( ( tup : (String, AggregationQuery) ) => {
            val (queryName, query) = tup
            val resList = cdrService.getAggregation( docId, query, date )
            (queryName, resList)
        } )

        Ok( results )
    } ) )

}
