package com.twosixlabs.dart.corpex.tools

import com.twosixlabs.dart.corpex.services.es.ElasticsearchSearchService
import com.twosixlabs.dart.corpex.services.kopa.KopaService
import com.twosixlabs.dart.rest.scalatra.models.HealthStatus

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object HealthCheck {

    def checkHealth( esService : ElasticsearchSearchService ) : (HealthStatus.HealthStatus, Option[ String ]) = {
        val esStatusFuture = esService.serviceCheck

        val resFuture = for {
            (esCheck, esMsg) <- esStatusFuture
        } yield {
            if (esCheck) (HealthStatus.HEALTHY, esMsg)
            else (HealthStatus.UNHEALTHY, esMsg)
        }

        Try( Await.result(resFuture, 10 seconds) ) match {
            case Success( res ) => res
            case Failure( _ ) => (HealthStatus.UNHEALTHY, Some( "Could not retrieve health status of search service" ))
        }
    }

    def checkHealth( esService : ElasticsearchSearchService, kopaService: KopaService ) : (HealthStatus.HealthStatus, Option[ String ]) = {
        val esStatusFuture = esService.serviceCheck
        val dbStatusFuture = kopaService.serviceCheck

        val resFuture = for {
            esStatus <- esStatusFuture
            dbStatus <- dbStatusFuture
        } yield {
            val (esCheck, esMsg) = esStatus
            val (dbCheck, dbMsg) = dbStatus
            val message = esMsg match {
                case Some( em : String ) => dbMsg match {
                    case Some( dm : String ) => Some( em + "; " + dm )
                    case None => Some( em )
                }
                case None => dbMsg match {
                    case Some( dm : String ) => Some( dm )
                    case None => None
                }
            }

            val status = {
                if(esCheck && dbCheck) HealthStatus.HEALTHY
                else if(esCheck && !dbCheck) HealthStatus.FAIR
                else if(esCheck && esMsg.isDefined) HealthStatus.FAIR
                else HealthStatus.UNHEALTHY
            }

            (status, message)
        }

        Try(Await.result( resFuture, 10 seconds )) match {
            case Success( res ) => res
            case Failure( _ ) => (HealthStatus.UNHEALTHY, Some( "Could not retrieve health status of search engine or aggregations index" ))
        }
    }

}
