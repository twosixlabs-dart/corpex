package com.twosixlabs.dart.corpex.controller

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.corpex.api.configuration.metadata.{DataIds, DataTypes}
import com.twosixlabs.dart.corpex.api.models.profiles.FieldProfile
import com.twosixlabs.dart.exceptions.ResourceNotFoundException
import org.slf4j.{Logger, LoggerFactory}

class DataController( dependencies : CorpexBaseController.Dependencies )
  extends CorpexBaseController( dependencies ) with SecureDartController {

    override val serviceName : String = "corpex"

    override val LOG : Logger = LoggerFactory.getLogger( getClass )

    get( "/" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        DataIds.values.filter( _.dataType != DataTypes.UNUSED ).map( dataId => {
            FieldProfile( dataId.apiLabel,
                          dataId.dataType.dataType,
                          dataId.cdrLabel,
                          dataId.humanLabel,
                          dataId.description )
        } ).toList.sortBy( _.fieldId )
    } ) )

    get( "/:fieldId" )( handleOutput( AuthenticateRoute.withUser { implicit user : DartUser =>
        val fieldId = params( "fieldId" )

        val fieldIdObj = DataIds.get( fieldId, _.apiLabel )
          .getOrElse( throw new ResourceNotFoundException( "field id", Some( fieldId ) ) )

        FieldProfile( fieldIdObj.apiLabel,
                      fieldIdObj.dataType.dataType,
                      fieldIdObj.cdrLabel,
                      fieldIdObj.humanLabel,
                      fieldIdObj.description )
    } ) )

}
