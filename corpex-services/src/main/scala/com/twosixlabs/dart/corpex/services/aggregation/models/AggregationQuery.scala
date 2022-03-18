package com.twosixlabs.dart.corpex.services.aggregation.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}
import com.twosixlabs.dart.corpex.services.aggregation.exceptions.QueryValidationException
import com.twosixlabs.dart.exceptions.BadQueryParameterException

import scala.beans.BeanProperty
import scala.util.{Failure, Success, Try}

@JsonInclude( Include.NON_ABSENT )
case class AggregationQuery( @BeanProperty @JsonProperty( "tag_id" ) tagId : Option[ String ] = None,
                             @BeanProperty @JsonProperty( "tag_type" ) tagType : Option[ String ] = None,
                             @BeanProperty @JsonProperty( "facet_id" ) facetId : Option[ String ] = None,
                             @BeanProperty @JsonProperty( "field_id" ) fieldId : Option[ String ] = None,
                             @BeanProperty @JsonProperty( "min_results" ) minResults : Option[ Int ] = None,
                             @BeanProperty @JsonProperty( "max_results" ) maxResults : Option[ Int ] = None ) {

    maxResults.foreach( max => if ( max < 1 ) throw new QueryValidationException( "maxResults", max.toString, "Max results must be more than 0" ) )
    for {mx <- maxResults; mn <- minResults} if ( mx < mn ) throw new QueryValidationException( "maxResults/minResults", s"${mx}/${mn}", "maxResults must be greater than or " +
                                                                                                                                         "equal to minResults" )

    if ( tagId.isDefined && tagType.isEmpty ) throw new QueryValidationException( "tagType", "<empty>", "tagType must be defined if tagId is defined" )
    if ( tagType.isDefined && tagId.isEmpty ) throw new QueryValidationException( "tagId", "<empty>", "tagId must be defined if tagType is defined" )
    if ( tagId.isDefined && facetId.isDefined ) throw new QueryValidationException( "tagId/facetId", s"${tagId.get}/${facetId.get}", "tagId and facetId cannot both be " +
                                                                                                                                     "defined" )
    if ( tagId.isDefined && fieldId.isDefined ) throw new QueryValidationException( "tagId/fieldId", s"${tagId.get}/${fieldId.get}", "tagId and fieldId cannot both be " +
                                                                                                                                     "defined" )
    if ( facetId.isDefined && fieldId.isDefined ) throw new QueryValidationException( "facetId/fieldId", s"${facetId.get}/${fieldId.get}", "facetId and fieldId cannot both" +
                                                                                                                                           " be defined" )

    def update( tagId : Option[ String ] = None,
                tagType : Option[ String ] = None,
                facetId : Option[ String ] = None,
                fieldId : Option[ String ] = None,
                minResults : Option[ String ] = None,
                maxResults : Option[ String ] = None ) : AggregationQuery =

        AggregationQuery.fromParams( if ( tagId.isDefined ) tagId else this.tagId,
                                     if ( tagType.isDefined ) tagType else this.tagType,
                                     if ( facetId.isDefined ) facetId else this.facetId,
                                     if ( fieldId.isDefined ) fieldId else this.fieldId,
                                     if ( minResults.isDefined ) minResults else this.minResults.map( _.toString ),
                                     if ( maxResults.isDefined ) maxResults else this.maxResults.map( _.toString ) )
}

object AggregationQuery {

    def fromParams( tagId : Option[ String ] = None,
                    tagType : Option[ String ] = None,
                    facetId : Option[ String ] = None,
                    fieldId : Option[ String ] = None,
                    minResults : Option[ String ] = None,
                    maxResults : Option[ String ] = None ) : AggregationQuery = {

        val minRes = Try( minResults.map( _.toInt ) ) match {
            case Success( o ) => o;
            case Failure( e : NumberFormatException ) => throw new BadQueryParameterException( "minResults", minResults, "Integer" )
            case Failure( e : Throwable ) => throw e
        }

        val maxRes = Try( maxResults.map( _.toInt ) ) match {
            case Success( o ) => o;
            case Failure( e : NumberFormatException ) => throw new BadQueryParameterException( "maxResults", maxResults, "Integer" )
            case Failure( e : Throwable ) => throw e
        }

        Try( AggregationQuery( tagId, tagType, facetId, fieldId, minRes, maxRes ) ) match {
            case Success( query : AggregationQuery ) => query
            case Failure( e : QueryValidationException ) => throw new BadQueryParameterException( e.param, Some( e.value ), e.format )
            case Failure( e : Throwable ) => throw e
        }
    }

}
