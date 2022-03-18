package com.twosixlabs.dart.corpex

import com.twosixlabs.dart.auth.controllers.SecureDartController
//import com.twosixlabs.dart.cdr.aggregator.controllers.CdrAggregationController
//import com.twosixlabs.dart.cdr.aggregator.services.{CdrService, ParameterizedQueryService}
import com.twosixlabs.dart.corpex.controller.{AnnotationsController, DataController, DocumentsController, SearchController}
import com.twosixlabs.dart.corpex.services.es.ElasticsearchSearchService
import com.twosixlabs.dart.rest.ApiStandards
import com.twosixlabs.dart.rest.scalatra.DartRootServlet
import com.twosixlabs.dart.search.ElasticsearchCorpusTenantIndex
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatra.LifeCycle
import org.slf4j.{Logger, LoggerFactory}

import javax.servlet.ServletContext
import scala.collection.JavaConverters._
import scala.util.Try

class ScalatraInit extends LifeCycle {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    val config : Config = ConfigFactory.defaultApplication().resolve()

    val esService : ElasticsearchSearchService = ElasticsearchSearchService( config )
    val esTenantIndex : ElasticsearchCorpusTenantIndex = ElasticsearchCorpusTenantIndex( config )

    val baseControllerDependencies : SecureDartController.Dependencies = {
        SecureDartController.deps( "corpex", config )
    }

    val allowedOrigins = Try( config.getString( "cors.allowed.origins" ) ).getOrElse( "*" )

    val basePath : String = ApiStandards.DART_API_PREFIX_V1 + "/corpex"

    val rootController = new DartRootServlet( Some( basePath ),
                                              Some( getClass.getPackage.getImplementationVersion ) )

    val annotationsController : AnnotationsController = new AnnotationsController( baseControllerDependencies )
    val dataController : DataController = new DataController( baseControllerDependencies )
    val searchController : SearchController = SearchController( esService, baseControllerDependencies )
    val documentsController : DocumentsController = DocumentsController( esService, esTenantIndex, baseControllerDependencies )
//    val aggregationController = {
//        val props : Map[String, String ] = System.getProperties.asScala.toMap
//        val queryService = new ParameterizedQueryService( props )
//        val cdrRepository = new HackyEsCdrRepository( esService )
//        val cdrService = new CdrService( props, cdrRepository )
//        new CdrAggregationController( queryService, cdrService )
//    }

    // Initialize scalatra: mounts servlets
    override def init( context : ServletContext ) : Unit = {
        context.setInitParameter( "org.scalatra.cors.allowedOrigins", allowedOrigins )
        context.mount( rootController, "/*" )
//        context.mount( aggregationController, basePath + "/cdr-aggregation/*" )
        context.mount( searchController, basePath + "/search/*" )
        context.mount( documentsController, basePath + "/documents/*" )
        context.mount( annotationsController, basePath + "/annotations/*" )
        context.mount( dataController, basePath + "/fields/*" )
    }

    // Scalatra callback to close out resources
    override def destroy( context : ServletContext ) : Unit = {
        super.destroy( context )
    }

}
