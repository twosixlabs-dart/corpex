package com.twosixlabs.dart.corpex.controller

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.corpex.api.configuration.annotations.{EntityTagTypes, EventTagTypes, FacetIds, TagIds, TagType}
import com.twosixlabs.dart.corpex.api.models.profiles.{FacetProfile, TagProfile, TagTypeProfile}
import com.twosixlabs.dart.corpex.controller
import com.twosixlabs.dart.exceptions.ResourceNotFoundException
import org.slf4j.{Logger, LoggerFactory}


class AnnotationsController( dependencies : controller.CorpexBaseController.Dependencies )
  extends CorpexBaseController( dependencies ) {

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    get( "/tags" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        TagIds.values.map( tagId => {
            TagProfile( tagId.apiLabel, tagId.cdrLabel, tagId.humanLabel, tagId.description )
        } ).toList.sortBy( _.tagId )
    } ) )

    get( "/tags/:tagId" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        val tagId = params( "tagId" )

        val tagIdObj = TagIds.get( tagId, _.apiLabel )
          .getOrElse( throw new ResourceNotFoundException( "tag id", Some( tagId ) ) )

        TagProfile( tagIdObj.apiLabel,
                    tagIdObj.cdrLabel,
                    tagIdObj.humanLabel,
                    tagIdObj.description )
    } ) )

    get( "/tags/:tagId/types" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        val tagId = params( "tagId" )
        val genProfile : TagType => TagTypeProfile = ( tagType : TagType ) => {
            TagTypeProfile( tagType.apiLabel,
                            tagType.cdrLabel,
                            tagType.humanLabel,
                            tagType.description )
        }

        tagId match {
            case TagIds.QNTFY_NER.apiLabel =>
                EntityTagTypes.values.map( genProfile ).toList.sortBy( _.tagType )
            case TagIds.QNTFY_EVENT.apiLabel =>
                EventTagTypes.values.map( genProfile ).toList.sortBy( _.tagType )
            case _ => throw new ResourceNotFoundException( "tag id", Some( tagId ) )
        }
    } ) )

    get( "/facets" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        FacetIds.values.map( facetId => {
            FacetProfile( facetId.apiLabel,
                          facetId.hasScore,
                          facetId.cdrLabel,
                          facetId.humanLabel,
                          facetId.description )
        } ).toList.sortBy( _.facetId )
    } ) )

    get( "/facets/:facetId" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        val facetIdParam = params( "facetId" )

        FacetIds.get( facetIdParam, _.apiLabel ) match {
            case Some( facetId ) =>
                FacetProfile( facetId.apiLabel,
                              facetId.hasScore,
                              facetId.cdrLabel,
                              facetId.humanLabel,
                              facetId.description )
            case None =>
                throw new ResourceNotFoundException( "facet id", Some( facetIdParam ) )
        }
    } ) )

}
