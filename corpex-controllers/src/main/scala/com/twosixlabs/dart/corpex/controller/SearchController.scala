package com.twosixlabs.dart.corpex.controller

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.permissions.DartOperations.SearchCorpus
import com.twosixlabs.dart.auth.tenant.{CorpusTenant, DartTenant, GlobalCorpus}
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.corpex.api.exceptions.{CorpexEnumException, InvalidAggQueryException, InvalidRequestException, InvalidSearchQueryException}
import com.twosixlabs.dart.corpex.api.models.CorpexSearchRequest
import com.twosixlabs.dart.corpex.api.tools.Mapper
import com.twosixlabs.dart.corpex.services.SearchService
import com.twosixlabs.dart.exceptions.{BadQueryParameterException, BadRequestBodyException}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

object SearchController {
    trait Dependencies extends CorpexBaseController.Dependencies {
        val searchService : SearchService

        def buildSearchController : SearchController = new SearchController( this )
        lazy val searchController : SearchController = buildSearchController
    }

    def apply(
        searchService : SearchService,
        baseDependencies : CorpexBaseController.Dependencies,
    ) : SearchController = {
        val ss = searchService
        new Dependencies {
            override val searchService : SearchService = ss
            override val serviceName : String = baseDependencies.serviceName
            override val secretKey : Option[String ] = baseDependencies.secretKey
            override val useDartAuth : Boolean = baseDependencies.useDartAuth
            override val basicAuthCredentials : Seq[ (String, String) ] = baseDependencies.basicAuthCredentials
        } buildSearchController
    }
}

class SearchController( dependencies : SearchController.Dependencies )
  extends CorpexBaseController( dependencies ) with SecureDartController {

    import dependencies._

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    post( "/" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        val reqBody : String = request.body
        LOG.info( reqBody )

        val searchRequest = Try( Mapper.unmarshal( reqBody, classOf[ CorpexSearchRequest ] ) ) match {
            case Success( req ) => req
            case Failure( e : InvalidRequestException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : InvalidSearchQueryException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : InvalidAggQueryException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : CorpexEnumException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : Throwable ) => throw e
        }

        val tenant = searchRequest.tenantId.map( DartTenant.fromString ).getOrElse( GlobalCorpus )

        SearchCorpus( tenant ).secureDart {
            searchService.search( searchRequest )
        }
    } ) )

    post( "/count" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        val reqBody : String = request.body
        LOG.info( reqBody )

        val searchRequest = Try( Mapper.unmarshal( reqBody, classOf[ CorpexSearchRequest ] ) ) match {
            case Success( req ) => req
            case Failure( e : InvalidRequestException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : InvalidSearchQueryException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : InvalidAggQueryException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : CorpexEnumException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : Throwable ) => throw e
        }

        val tenant = searchRequest.tenantId.map( DartTenant.fromString ).getOrElse( GlobalCorpus )

        SearchCorpus( tenant ).secureDart {
            searchService.count( searchRequest )
        }
    } ) )

    post( "/shave" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        val takeParam = params.get( "take" ).map( _.toInt )
        val reqBody : String = request.body

        val searchRequest = Try( Mapper.unmarshal( reqBody, classOf[ CorpexSearchRequest ] ) ) match {
            case Success( req ) => req
            case Failure( e : InvalidRequestException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : InvalidSearchQueryException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : InvalidAggQueryException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : CorpexEnumException ) => throw new BadRequestBodyException( e.getMessage )
            case Failure( e : Throwable ) => throw e
        }

        val tenant = searchRequest.tenantId.map( DartTenant.fromString ).getOrElse( GlobalCorpus )

        SearchCorpus( tenant ).secureDart {
            val take = takeParam
              .getOrElse( searchRequest.pageSize
                            .getOrElse( throw new BadQueryParameterException( """Either the request body should include the 'page_size' field, or the query string should include the 'take' paramter""" ) ))

            searchService.shave(  searchRequest, take )
        }
    } ) )

}
