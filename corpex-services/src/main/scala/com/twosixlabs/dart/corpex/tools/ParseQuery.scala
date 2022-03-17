package com.twosixlabs.dart.corpex.tools

import com.twosixlabs.dart.corpex.api.configuration.metadata.{DataId, DataIds, DataType, DataTypes}
import com.twosixlabs.dart.corpex.api.models.queries.{CorpexAggQuery, CorpexFieldAggQuery}

object ParseQuery {
    def getDataIdFromAggQuery( corpexAggQuery : CorpexAggQuery ) : DataId = corpexAggQuery match {
        case CorpexFieldAggQuery( queriedField, _, _, _, _, _ ) =>
            DataIds.get( queriedField, _.apiLabel ).get

        case _ => throw new IllegalStateException( "No data id!" )
    }


    def getDataTypeFromAggQuery( corpexAggQuery : CorpexAggQuery ) : DataType = corpexAggQuery match {
        case CorpexFieldAggQuery( queriedField, _, _, _, _, _ ) =>
            DataIds.get( queriedField, _.apiLabel ).get.dataType

        case _ => DataTypes.TERM
    }


}
