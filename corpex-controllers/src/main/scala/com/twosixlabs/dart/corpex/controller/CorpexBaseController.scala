package com.twosixlabs.dart.corpex.controller

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.rest.scalatra.{AsyncDartScalatraServlet, DartScalatraServlet}
import org.scalatra.CorsSupport
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

object CorpexBaseController {
    trait Dependencies extends SecureDartController.Dependencies

    implicit class FromAuthDeps( sdcDeps : SecureDartController.Dependencies ) extends Dependencies {
        override val serviceName : String = sdcDeps.serviceName
        override val secretKey : Option[String ] = sdcDeps.secretKey
        override val useDartAuth : Boolean = sdcDeps.useDartAuth
        override val basicAuthCredentials : Seq[ (String, String) ] = sdcDeps.basicAuthCredentials
    }
}

abstract class CorpexBaseController( dependencies : CorpexBaseController.Dependencies )
  extends DartScalatraServlet with CorsSupport with SecureDartController {

    override val serviceName : String = dependencies.serviceName
    override val secretKey : Option[ String ] = dependencies.secretKey
    override val useDartAuth : Boolean = dependencies.useDartAuth
    override val basicAuthCredentials : Seq[ (String, String) ] = dependencies.basicAuthCredentials

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    setStandardConfig()

}

abstract class AsyncCorpexBaseController( dependencies : CorpexBaseController.Dependencies )
  extends AsyncDartScalatraServlet with CorsSupport with SecureDartController {

    override val serviceName : String = dependencies.serviceName
    override val secretKey : Option[ String ] = dependencies.secretKey
    override lazy val useDartAuth : Boolean = dependencies.useDartAuth
    override lazy val basicAuthCredentials : Seq[ (String, String) ] = dependencies.basicAuthCredentials

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    protected implicit def executor : ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    setStandardConfig()

}
