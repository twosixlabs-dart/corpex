package com.twosixlabs.dart.corpex.tools

import com.twosixlabs.dart.corpex.services.search.es.ElasticsearchSearchService
import com.twosixlabs.dart.rest.scalatra.models.HealthStatus

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

object HealthCheck {

    def checkHealth( esService : ElasticsearchSearchService ) : (HealthStatus.HealthStatus, Option[ String ]) = {
        val esStatusFuture = esService.serviceCheck

        val resFuture = for {
            (esCheck, esMsg) <- esStatusFuture
        } yield {
            if ( esCheck ) (HealthStatus.HEALTHY, esMsg)
            else (HealthStatus.UNHEALTHY, esMsg)
        }

        Try( Await.result(resFuture, 10 seconds) ) match {
            case Success( res ) => res
            case Failure( _ ) => (HealthStatus.UNHEALTHY, Some( "Could not retrieve health status of search service" ))
        }
    }

}
