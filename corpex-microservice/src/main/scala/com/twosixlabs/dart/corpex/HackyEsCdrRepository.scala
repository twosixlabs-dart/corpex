package com.twosixlabs.dart.corpex

import com.twosixlabs.cdr4s.api.repository.CdrRepository
import com.twosixlabs.cdr4s.core.{CdrAnnotation, CdrDocument}
import com.twosixlabs.cdr4s.json.dart.DartJsonFormat
import com.twosixlabs.dart.corpex.api.tools.Mapper
import com.twosixlabs.dart.corpex.services.SearchService

import scala.util.Try

class HackyEsCdrRepository( searchService : SearchService ) extends CdrRepository {

    override def upsertAnnotation( docId : String, annotation : CdrAnnotation[ _ ],
    timestamp : Long ) : Try[ CdrAnnotation[ _ ] ] = ???

    override def updateExtractedText( docId : String, updatedText : String,
    timestamp : Long ) : Try[ String ] = ???

    override def upsertFullCdr( doc : CdrDocument,
    timestamp : Long ) : Try[ CdrDocument ] = ???

    override def delete( docId : String ) : Try[ String ] = ???

    override def getFullCdr( docId : String,
    timestamp : Long ) : Try[ Option[ CdrDocument ] ] = {
    if ( timestamp != CdrRepository.LATEST_VERSION ) throw new NotImplementedError( "Cannot query search repository by timestamp" )
    Try( new DartJsonFormat().unmarshalCdr( Mapper.marshal( searchService.getDocument( docId ) ) ) )
}

    override def getContent( docId : String,
    timestamp : Long ) : Try[ Option[ CdrDocument ] ] = ???

    override def getMetadata( docId : String,
    timestamp : Long ) : Try[ Option[ CdrDocument ] ] = ???

    override def getAnnotations( docId : String,
    timestamp : Long ) : Try[ Option[ CdrDocument ] ] = ???

    override def exists( docId : String ) : Try[ Boolean ] = ???

    override def count( ) : Try[ Long ] = ???

    override def getAll( timestamp : Long ) : Stream[ CdrDocument ] = ???

    override def getAllBySection( sections : Seq[ String ], timestamp : Long ) : Stream[ CdrDocument ] = ???

    override def getAllByField( fields : Seq[ (String, String) ],
    timestamp : Long ) : Stream[ CdrDocument ] = ???
}
