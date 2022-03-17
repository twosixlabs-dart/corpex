package com.twosixlabs.dart.corpex.api.configuration.annotations

import com.twosixlabs.dart.corpex.api.configuration.ConfigEnum

sealed case class FacetId( cdrLabel : String,
                           apiLabel : String,
                           hasScore : Boolean,
                           humanLabel : Option[ String ] = None,
                           description : Option[ String ] = None )

object FacetIds extends ConfigEnum[ FacetId ] {

    val QNTFY_TOPIC : FacetId = V( FacetId( cdrLabel = "qntfy-categories-annotator", apiLabel = "qntfy-topic", hasScore = true, humanLabel = Some( "Qntfy Topics" ), description = Some( "Predicted topics/categories with confidence scores" ) ) )
    val QNTFY_SENTIMENT : FacetId = V( FacetId( cdrLabel = "qntfy-sentiment-annotator", apiLabel = "qntfy-sentiment", hasScore = true, humanLabel = Some( "Qntfy Sentiment/Subjectivity" ), description = Some( "Bias and stance detection" ) ) )
    val FACTIVA_REGION : FacetId = V( FacetId( cdrLabel = "factiva-regions", apiLabel = "factiva-region", hasScore = false, humanLabel = Some( "Factiva Regions" ), description =  Some( "Contextual regions identified by Dow Jones' Factiva service (Factiva docs only)" ) ) )
    val FACTIVA_SUBJECT : FacetId = V( FacetId( cdrLabel = "factiva-subjects", apiLabel = "factiva-subject", hasScore = false, humanLabel = Some( "Factiva Subjects" ), description =  Some( "Subjects identified by Dow Jones' Factiva service (Factiva docs only" ) ) )
    val FACTIVA_INDUSTRY : FacetId = V( FacetId( cdrLabel = "factiva-industries", apiLabel = "factiva-industry", hasScore = false, humanLabel = Some( "Factiva Industries" ), description =  Some( "Industries identified by Dow Jones' Factiva service (Factiva docs only" ) ) )

    def fromCdr( cdrLabel : String ) : FacetId = values.filter( _.cdrLabel == cdrLabel ).toList.head

    def fromApi( apiLabel : String ) : FacetId = values.filter( _.apiLabel == apiLabel ).toList.head

}
