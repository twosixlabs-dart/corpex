package com.twosixlabs.dart.corpex.api.configuration.metadata

import com.twosixlabs.dart.corpex.api.configuration.ConfigEnum
import com.twosixlabs.dart.corpex.api.models.MultiValue

sealed abstract class DataId {
    val defaultBucketSize : Option[ MultiValue ]
    val apiLabel : String
    val cdrLabel : Option[ String ]
    val dataType : DataType
    val humanLabel: Option[ String ] = None
    val description: Option[ String ] = None
}

sealed case class CdrDataId( override val dataType : DataType,
                             override val apiLabel : String,
                             override val cdrLabel : Option[ String ] = None,
                             override val defaultBucketSize : Option[ MultiValue ] = None,
                             override val humanLabel : Option[ String ] = None,
                             override val description : Option[ String ] = None ) extends DataId

sealed case class CorpexDataId( override val dataType : DataType,
                                override val apiLabel : String,
                                override val cdrLabel : Option[ String ] = None,
                                override val defaultBucketSize : Option[ MultiValue ] = None,
                                override val humanLabel : Option[ String ] = None,
                                override val description : Option[ String ] = None ) extends DataId

object DataIds extends ConfigEnum[ DataId ] {
    def genCdrId( cdrLabel : String,
                  dataType : DataType,
                  bucketSize : Option[ MultiValue ] = None,
                  humanLabel : Option[ String ] = None,
                  description : Option[ String ] = None ) : CdrDataId = {
        CdrDataId( dataType, "cdr." + cdrLabel.trim, Some( cdrLabel.trim ), bucketSize, humanLabel, description )
    }

    def genCorpexId( apiLabel : String,
                     cdrLabel : String,
                     dataType : DataType,
                     bucketSize : Option[ MultiValue ] = None,
                     humanLabel : Option[ String ] = None,
                     description : Option[ String ] = None ) : CorpexDataId = {
        CorpexDataId( dataType, apiLabel, Some( cdrLabel ), bucketSize, humanLabel, description )
    }

    def genUnused( cdrLabel : String ) : CdrDataId = {
        genCdrId( cdrLabel, DataTypes.UNUSED )
    }


    val EXTRACTED_NUMERIC = V( genUnused( "extracted_numeric" ) )
    val URI = V( genUnused( "uri" ) )
    val NTRIPLES = V( genUnused( "extracted_ntriples" ) )
    val EXTRACTED_TEXT = V( genCdrId( "extracted_text", DataTypes.TEXT ) )
    val TITLE = V( genCdrId( "extracted_metadata.Title", DataTypes.TEXT, humanLabel = Some( "Title" ) ) )
    val DESCRIPTION = V( genCdrId( "extracted_metadata.Description", DataTypes.TEXT, humanLabel = Some( "Description" ) ) )
    val AUTHOR = V( genCdrId( "extracted_metadata.Author", DataTypes.TEXT, humanLabel = Some( "Author" ) ) )
    val CONTENT_TYPE = V( genCdrId( "content_type", DataTypes.TERM, humanLabel = Some( "Content Type" ) ) )
    val CAPTURE_SOURCE = V( genCdrId( "capture_source", DataTypes.TERM, humanLabel = Some( "Capture Source" ), description = Some( "Source of ingestion: AutoCollection means obtained through automation or subscription, BackgroundSource means user-uploaded" ) ) )
    val DOC_TYPE = V( genCdrId( "extracted_metadata.Type", DataTypes.TERM, humanLabel = Some( "Document Type" ) ) )
    val ORIGINAL_LANGUAGE = V( genCdrId( "extracted_metadata.OriginalLanguage", DataTypes.TERM, humanLabel = Some( "Original Language" ) ) )
    val CLASSIFICATION = V( genCdrId( "extracted_metadata.Classification", DataTypes.TERM, humanLabel = Some( "Classification" ) ) )
    val PUBLISHER = V( genCdrId( "extracted_metadata.Publisher", DataTypes.TERM, humanLabel = Some( "Publisher" ) ) )
    val PRODUCER = V( genCdrId( "extracted_metadata.Producer", DataTypes.TERM, humanLabel = Some( "Producer" ) ) )
    val SUBJECT = V( genCdrId( "extracted_metadata.Subject", DataTypes.TERM, humanLabel = Some( "Subject" ) ) )
    val CREATOR = V( genCdrId( "extracted_metadata.Creator", DataTypes.TERM, humanLabel = Some( "Creator" ) ) )
    val LABELS = V( genCdrId( "labels", DataTypes.TERM, humanLabel = Some( "Labels" ), description = Some( "User-defined identifiers (tags)") ) )
    val STATED_GENRE = V( genCdrId( "extracted_metadata.StatedGenre", DataTypes.TERM, humanLabel = Some( "User-Defined Genre" ), description = Some( "Type of document (e.g., academic article, policy paper, news article, etc. Provided by user, not machine learning analytic" ) ) )
    val PREDICTED_GENRE = V( genCdrId( "extracted_metadata.PredictedGenre", DataTypes.TERM, humanLabel = Some( "Predicted Genre" ), description = Some( "Type of document (e.g., academic article, policy paper, news article, etc. Predicted by machine learning analytic" ) ) )
    val DOC_ID = V( genCdrId( "document_id", DataTypes.TERM, humanLabel = Some( "Document Id" ), description = Some( "Canonical identifier of a document, derived from md5 hash of the raw document" ) ) )
    val SOURCE_URI = V( genCdrId( "source_uri", DataTypes.TERM, humanLabel = Some( "Source URI" ), description = Some( "Currently the filename of the raw document as it was submitted to DART" ) ) )
    val CREATION_DATE = V( genCdrId( "extracted_metadata.CreationDate", DataTypes.DATE, Some( MultiValue( string = Some( "1y" ) ) ), humanLabel = Some( "Publication Date" ) ) )
    val MOD_DATE = V( genCdrId( "extracted_metadata.ModDate", DataTypes.DATE, Some( MultiValue( string = Some( "1y" ) ) ), humanLabel = Some( "Modification Date" ) ) )
    val PAGES = V( genCdrId( "extracted_metadata.Pages", DataTypes.INT, Some( MultiValue( long = Some( 5L ) ) ), humanLabel = Some( "Pages" ) ) )
    val TIMESTAMP = V( genCdrId( "timestamp", DataTypes.DATE, Some( MultiValue( string = Some( "1M" ) ) ), humanLabel = Some( "Timestamp" ), description = Some( "Date and time of ingestion in DART" ) ) )
    val WORD_COUNT = V( genCorpexId( "word_count", "extracted_text.length", DataTypes.INT, Some( MultiValue( long = Some( 500 ) ) ), humanLabel = Some( "Word Count" ) ) )

}
