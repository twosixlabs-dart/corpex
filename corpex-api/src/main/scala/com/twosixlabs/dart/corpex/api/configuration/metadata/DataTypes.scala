package com.twosixlabs.dart.corpex.api.configuration.metadata

import com.twosixlabs.dart.corpex.api.configuration.ConfigEnum

sealed case class DataType( dataType : String )

object DataTypes extends ConfigEnum[ DataType ] {

    val DATE = V( DataType( "date" ) )
    val INT = V( DataType( "int" ) )
    val TERM = V( DataType( "term" ) )
    val TEXT = V( DataType( "text" ) )
    val UNUSED = V( DataType( "unused" ) )

}
