package com.twosixlabs.dart.corpex.api.configuration.annotations

import com.twosixlabs.dart.corpex.api.configuration.ConfigEnum

sealed case class TagId( cdrLabel : String,
                         apiLabel : String,
                         humanLabel : Option[ String ] = None,
                         description : Option[ String ] = None )

object TagIds extends ConfigEnum[ TagId ] {

    val QNTFY_NER : TagId = V( TagId( cdrLabel = "qntfy-ner-annotator", apiLabel = "qntfy-ner", humanLabel = Some( "Qntfy NER" ), description = Some( "Named entity extractions" ) ) )
    val QNTFY_EVENT : TagId = V( TagId( cdrLabel = "qntfy-events-annotator", apiLabel = "qntfy-event", humanLabel = Some( "Qntfy Events" ), description = Some( "Detected events extractions" ) ) )

    def fromCdr( cdrLabel : String ) : TagId = values.filter( _.cdrLabel == cdrLabel ).toList.head

    def fromApi( apiLabel : String ) : TagId = values.filter( _.apiLabel == apiLabel ).toList.head

}



