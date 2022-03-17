package com.twosixlabs.dart.corpex.controller

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.permissions.DartOperations.RetrieveDocument
import com.twosixlabs.dart.auth.tenant.CorpusTenantIndex.CorpusTenantIndexException
import com.twosixlabs.dart.auth.tenant.{CorpusTenant, CorpusTenantIndex, DartTenant, GlobalCorpus}
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.corpex.services.SearchService
import com.twosixlabs.dart.exceptions.BadQueryParameterException
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object DocumentsController {
    trait Dependencies extends CorpexBaseController.Dependencies {
        val searchService : SearchService
        val tenantIndex : CorpusTenantIndex

        def buildDocumentsController : DocumentsController = new DocumentsController( this )
        lazy val documentsController : DocumentsController = buildDocumentsController
    }

    def apply(
        searchService : SearchService,
        tenantIndex : CorpusTenantIndex,
        baseDependencies : CorpexBaseController.Dependencies,
    ) : DocumentsController = {
        val ss = searchService; val ti = tenantIndex
        new Dependencies {
            override val searchService : SearchService = ss
            override val tenantIndex : CorpusTenantIndex = ti
            override val serviceName : String = baseDependencies.serviceName
            override val secretKey : Option[String ] = baseDependencies.secretKey
            override val bypassAuth : Boolean = baseDependencies.bypassAuth
        } buildDocumentsController
    }
}

class DocumentsController( dependencies : DocumentsController.Dependencies )
  extends CorpexBaseController( dependencies ) with SecureDartController {

    import dependencies._

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    get( "/:id" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        val id = params( "id" )
        val fieldsIncl = params.get( "fieldsIncl" )
        val fieldsExcl = params.get( "fieldsExcl" )
        val tenant = params.get( "tenant" ).map( t => {
            val tId = t.trim.toLowerCase
            val tenant = DartTenant.fromString( tId )
            if ( tenant == GlobalCorpus ) tenant
            else Try( Await.result( tenantIndex.tenant( tId ), 20.seconds ) ) match {
                case Success( res ) => res
                case Failure( e : CorpusTenantIndexException ) => throw new BadQueryParameterException( List( "tenant" ), Some( e.getMessage ) )
                case Failure( e ) => throw e
            }
        } ).getOrElse( GlobalCorpus )

        RetrieveDocument.from( tenant ).secureDart {
            searchService.getDocument( id, fieldsIncl, fieldsExcl )
        }
    } ) )

}
