package com.twosixlabs.dart.corpex.services.search.es

import java.io.IOException
import java.util.concurrent.TimeUnit
import com.twosixlabs.cdr4s.json.dart.{ DartCdrDocumentDto, DartMetadataDto }
import com.twosixlabs.dart.corpex.api.models.{ CorpexSearchRequest, CorpexSearchResults, CorpexSingleResult }
import com.twosixlabs.dart.corpex.api.tools.Mapper
import com.twosixlabs.dart.corpex.services.search.es.models.EsDocumentResponse
import com.twosixlabs.dart.corpex.services.search.SearchService
import com.twosixlabs.dart.corpex.services.search.es.models.{ EsCountResponse, EsDocumentResponse, EsScrollRequest, EsSearchResponse }
import com.twosixlabs.dart.exceptions.{ BadQueryParameterException, Exceptions, ResourceNotFoundException, ServiceUnreachableException }
import com.typesafe.config.Config
import okhttp3.{ MediaType, OkHttpClient, Request, RequestBody, Response }
import org.slf4j.{ Logger, LoggerFactory }

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ElasticsearchSearchService {

    trait Dependencies extends SearchService.DI {
        val esHost : String
        val esPort : Int
        val scrollSize : Int
        val scrollTimeout : String
        val okHttpClient : OkHttpClient

        def buildEsSearchService : ElasticsearchSearchService = new ElasticsearchSearchService( this )
        lazy val esSearchService : ElasticsearchSearchService = buildEsSearchService

        override def buildSearchService : SearchService = buildEsSearchService
    }

    def apply(
        esHost : String,
        esPort : Int,
        scrollSize : Int,
        scrollTimeout : String,
        okHttpClient: OkHttpClient,
        defaultPageSize : Int,
        baseFields : List[ String ],
        defaultTextField : String,
    ) : ElasticsearchSearchService = {
        val eh = esHost; val ep = esPort; val ss = scrollSize; val st = scrollTimeout; val okc = okHttpClient
        val dps = defaultPageSize; val bf = baseFields; val dtf = defaultTextField
        new Dependencies {
            override val esHost : String = eh
            override val esPort : Int = ep
            override val scrollSize : Int = ss
            override val scrollTimeout : String = st
            override val okHttpClient : OkHttpClient = okc
            override val defaultPageSize : Int = dps
            override val baseFields : List[String ] = bf
            override val defaultTextField : String = dtf
        } buildEsSearchService
    }

    def apply(
        esHost : String,
        esPort : Int,
        scrollSize : Int,
        scrollTimeout : String,
        okHttpClient: OkHttpClient,
        searchServiceDependencies : SearchService.Dependencies,
    ) : ElasticsearchSearchService = apply(
        esHost, esPort, scrollSize, scrollTimeout, okHttpClient,
        searchServiceDependencies.defaultPageSize,
        searchServiceDependencies.baseFields,
        searchServiceDependencies.defaultTextField,
    )

    def apply(
        okHttpClient : OkHttpClient,
        config : Config,
    ) : ElasticsearchSearchService = apply(
        config.getString( "elasticsearch.host" ),
        config.getInt( "elasticsearch.port" ),
        config.getInt( "corpex.scroll.size" ),
        config.getString( "corpex.scroll.timeout" ),
        okHttpClient,
        SearchService.deps( config )
    )

    def apply( config : Config ) : ElasticsearchSearchService = {
        apply( new OkHttpClient(), config )
    }
}

class ElasticsearchSearchService( dependencies : ElasticsearchSearchService.Dependencies ) extends SearchService {

    override val baseFields : List[ String ] = dependencies.baseFields
    override val defaultPageSize : Int = dependencies.defaultPageSize
    override val defaultTextField : String = dependencies.defaultTextField

    import dependencies._

    lazy val conv = new EsCorpexConvert( defaultPageSize, baseFields, defaultTextField )

    lazy val esUrl = s"http://$esHost:$esPort/cdr_search/_search"
    lazy val esCountUrl = s"http://$esHost:$esPort/cdr_search/_count"
    lazy val esDocBaseUrl = s"http://$esHost:$esPort/cdr_search/_doc"

    private val LOG : Logger = LoggerFactory.getLogger( getClass )
    private val JSON : MediaType = MediaType.get( "application/json; charset=utf-8" )

    val allFields : Set[ String ] = Set(
        "capture_source",
        "uri",
        "source_uri",
        "document_id",
        "timestamp",
        "content_type",
        "extracted_metadata",
        "extracted_metadata.CreationDate",
        "extracted_metadata.ModDate",
        "extracted_metadata.Author",
        "extracted_metadata.Title",
        "extracted_metadata.Description",
        "extracted_metadata.URL",
        "extracted_metadata.Type",
        "extracted_metadata.Classification",
        "extracted_metadata.Language",
        "extracted_metadata.Pages",
        "extracted_metadata.Subject",
        "extracted_metadata.Creator",
        "extracted_metadata.Producer",
        "extracted_metadata.Publisher",
        "extracted_ntriples",
        "extracted_text",
        "extracted_numeric",
        "annotations",
        "categories"
     )

    def search( corpexRequest : CorpexSearchRequest ) : CorpexSearchResults = {
        val resPerPage = corpexRequest.pageSize.getOrElse( defaultPageSize )
        val page = corpexRequest.page.getOrElse( 0 )

        try {
            val esRequest = conv.corpexRequestToEsRequest( corpexRequest )

            val requestJson = Mapper.marshal( esRequest )
            LOG.info( "New ES request:" )
            LOG.info( requestJson )

            val body = RequestBody.create( requestJson, JSON )
            val request : Request = new Request.Builder().url( esUrl ).post( body ).build()
            val response : Response = okHttpClient.newCall( request ).execute()

            response.code() match {
                // deserialize json (response.body.string()) into DuplicateProbability object using Jackson mapper
                case 200 =>
                    val resJson = response.body.string()
                    val esRes = Mapper.unmarshal( resJson, classOf[ EsSearchResponse ] )

                    conv.esResponseToCorpexResults( esRes, corpexRequest )

                case _ =>
                    LOG.error( s"Error executing remote REST request : ${response.code} : ${response.body.string}" )
                    throw new ServiceUnreachableException( "search datastore" )
            }
        } catch {
            case e : IOException =>
                LOG.error( s"Exception caught trying to communicate with service: ${e.getMessage} : ${e.getCause}" )
                LOG.error( Exceptions.getStackTraceText( e ) )
                throw new ServiceUnreachableException( "search datastore" )

        }

    }

    def count( corpexRequest : CorpexSearchRequest ) : CorpexSearchResults = {

        val esRequest = conv.corpexRequestToEsCountRequest( corpexRequest )

        val requestJson = Mapper.marshal( esRequest )
        LOG.info( "New ES count request:" )
        LOG.info( requestJson )

        try {
            val body = RequestBody.create( requestJson, JSON )
            val request : Request = new Request.Builder().url( esCountUrl ).post( body ).build()
            val response : Response = okHttpClient.newCall( request ).execute()

            response.code() match {
                // deserialize json (response.body.string()) into DuplicateProbability object using Jackson mapper
                case 200 =>
                    val resJson = response.body.string()
                    val esRes = Mapper.unmarshal( resJson, classOf[ EsCountResponse ] )

                    if ( esRes.count.isDefined ) CorpexSearchResults( numResults = esRes.count.get, exactNum = true )
                    else {
                        LOG.error( s"ES did not return count field for some reason\nFull response:" )
                        LOG.error( resJson )
                        throw new ServiceUnreachableException( "search datastore" )
                    }

                case _ =>
                    val resBody = response.body.string
                    LOG.error( s"Error executing remote REST request : ${response.code} : ${resBody}" )
                    throw new ServiceUnreachableException( "search datastore" )
            }
        } catch {
            case e : IOException =>
                LOG.error( s"Exception caught trying to communicate with service: ${e.getMessage} : ${e.getCause}" )
                LOG.error( Exceptions.getStackTraceText( e ) )
                throw new ServiceUnreachableException( "search datastore" )

        }
    }

    def getDocument( id : String, fieldsIncl : Option[ String ] = None, fieldsExcl : Option[ String ] = None ) : DartCdrDocumentDto = {
        try {
            val incl = fieldsIncl.map( fields => {
                val fieldsList = fields.split( "," ).map( _.trim )
                fieldsList.foreach( field => {
                    if ( !allFields.contains( field ) )
                        throw new BadQueryParameterException( "fieldsIncl", fieldsIncl, s"${field} is not a valid CDR field" )
                } )
                fieldsList.mkString( "," )
            } )

            val excl = fieldsExcl.map( fields => {
                val fieldsList = fields.split( "," ).map( _.trim )
                fieldsList.foreach( field => {
                    if ( !allFields.contains( field ) )
                        throw new BadQueryParameterException( "fieldsExcl", fieldsExcl, s"${field} is not a valid CDR field" )
                } )
                fieldsList.mkString( "," )
            } )

            val url = s"${esDocBaseUrl}/${id}?_source_includes=${incl.getOrElse( "" )}&_source_excludes=${excl.getOrElse( "" )}"

            val request : Request = new Request.Builder().url( url ).get().build()
            val response : Response = okHttpClient.newCall( request ).execute()
            val resJson = response.body.string()

            response.code() match {
                // deserialize json (response.body.string()) into DuplicateProbability object using Jackson mapper
                case 200 =>
                    val esRes = Mapper.unmarshal( resJson, classOf[ EsDocumentResponse ] )
                    esRes.dartDoc.cdr

                case 404 =>
                    throw new ResourceNotFoundException( "cdr document", Some( id ) )

                case _ =>
                    LOG.error( s"Error executing remote REST request : ${response.code} : ${resJson}" )
                    throw new ServiceUnreachableException( "search datastore" )
            }
        } catch {
            case e : IOException =>
                LOG.error( s"Exception caught trying to communicate with service: ${e.getMessage} : ${e.getCause}" )
                LOG.error( Exceptions.getStackTraceText( e ) )
                throw new ServiceUnreachableException( "search datastore" )

        }
    }

    // No longer needed, since Elasticsearch can do the fields filtering for us, but holding onto it
    // in case we need to implement it ourselves at some point
    def getCroppedCdr( dto : DartCdrDocumentDto, fields : Set[ String ] ) : DartCdrDocumentDto = {

        val metadata = {
            val md = dto.extractedMetadata
            if ( !fields.contains( "extracted_metadata" ) || dto.extractedMetadata == null ) null
            else DartMetadataDto(
                creationDate = if ( fields.contains( "extracted_metadata.CreationDate" ) ) md.creationDate else null,
                modificationDate = if ( fields.contains( "extracted_metadata.ModDate" ) ) md.modificationDate else null,
                author = if ( fields.contains( "extracted_metadata.Author" ) ) md.author else null,
                title = if ( fields.contains( "extracted_metadata.Title" ) ) md.title else null,
                description = if ( fields.contains( "extracted_metadata.Description" ) ) md.description else null,
                url = if ( fields.contains( "extracted_metadata.URL" ) ) md.url else null,
                docType = if ( fields.contains( "extracted_metadata.Type" ) ) md.docType else null,
                classification = if ( fields.contains( "extracted_metadata.Classification" ) ) md.classification else null,
                originalLanguage = if ( fields.contains( "extracted_metadata.OriginalLanguage" ) ) md.originalLanguage else null,
                pages = if ( fields.contains( "extracted_metadata.Pages" ) ) md.pages else null,
                subject = if ( fields.contains( "extracted_metadata.Subject" ) ) md.subject else null,
                creator = if ( fields.contains( "extracted_metadata.Creator" ) ) md.creator else null,
                producer = if ( fields.contains( "extracted_metadata.Producer" ) ) md.producer else null,
                publisher = if ( fields.contains( "extracted_metadata.Publisher" ) ) md.publisher else null )
        }

        DartCdrDocumentDto(
            uri = if ( fields.contains( "uri" ) ) dto.uri else null,
            captureSource = if ( fields.contains( "capture_source" ) ) dto.captureSource else null,
            sourceUri = if ( fields.contains( "source_uri" ) ) dto.sourceUri else null,
            documentId = if ( fields.contains( "document_id" ) ) dto.documentId else null,
            timestamp = if ( fields.contains( "timestamp" ) ) dto.timestamp else null,
            contentType = if ( fields.contains( "content_type" ) ) dto.contentType else null,
            extractedMetadata = metadata,
            extractedNtriples = if ( fields.contains( "extracted_ntriples" ) ) dto.extractedNtriples else null,
            extractedText = if ( fields.contains( "extracted_text" ) ) dto.extractedText else null,
            extractedNumeric = if ( fields.contains( "extracted_numeric" ) ) dto.extractedNumeric else null,
            annotations = if ( fields.contains( "annotations" ) ) dto.annotations else null,
            labels = if ( fields.contains( "labels" ) ) dto.labels else null )
    }

    def shave( searchRequest: CorpexSearchRequest, take : Int ) : List[ String ] = {
        val pageSize = if ( take > 1000 ) 1000 else take
        val shaveRequest = searchRequest.copy( page = None,
                                               pageSize = Some( pageSize ),
                                               fields = Some( List( "cdr.document_id" ) ),
                                               aggs = None )

        val resultsBuffer = mutable.Buffer[ CorpexSingleResult ]()
        val (firstResults, scrollId) = scrollSearchStart( shaveRequest )
        resultsBuffer ++= firstResults.results.get

        var continue = true
        while ( continue && resultsBuffer.length < take ) {
            val nextResults = scrollSearchContinue( scrollId, searchRequest ).results.get
            if ( nextResults.isEmpty ) continue = false
            else resultsBuffer ++= nextResults
        }

        resultsBuffer.take( take ).toList.map( _.cdr.documentId )
    }

    private def scrollSearchStart( req : CorpexSearchRequest ) : (CorpexSearchResults, String) = {
        val startScrollUrl = s"${esUrl}?scroll=${scrollTimeout}"

        LOG.info( s"starting a scroll: ${startScrollUrl}" )

        val esReq = conv.corpexRequestToEsRequest( req )

        val requestJson = Mapper.marshal( esReq )
        LOG.info( "New ES request:" )
        LOG.info( requestJson )

        try {
            val body = RequestBody.create( requestJson, JSON )
            val request : Request = new Request.Builder().url( startScrollUrl ).post( body ).build()
            val response : Response = okHttpClient.newCall( request ).execute()

            response.code() match {
                // deserialize json (response.body.string()) into DuplicateProbability object using Jackson mapper
                case 200 =>
                    val resJson = response.body.string()
                    val esRes = Mapper.unmarshal( resJson, classOf[ EsSearchResponse ] )

                    val corpexResults = conv.esResponseToCorpexResults( esRes, req )

                    if ( esRes.scrollId.isEmpty ) {
                        LOG.error( "Scroll initiation failed to return scroll id" )
                        throw new Exception( "Unable to create scroll context in Elasticsearch" )
                    }
                    else (corpexResults, esRes.scrollId.get)

                case _ =>
                    LOG.error( s"Error executing remote REST request : ${response.code} : ${response.body.string}" )
                    throw new Exception( s"Elasticsearch error\n${response.code} : ${response.body.string}" )
            }
        } catch {
            case e : IOException =>
                LOG.error( s"Exception caught trying to communicate with service: ${e.getMessage} : ${e.getCause}" )
                throw new Exception( s"Error submitting Elasticsearch request\n${e.getMessage} : ${e.getCause}" )
        }
    }

    private def scrollSearchContinue( id : String, corpexRequest : CorpexSearchRequest ) : CorpexSearchResults = {
        val continueScrollUrl = s"http://$esHost:$esPort/_search/scroll"

        val esReq = EsScrollRequest( Some( scrollTimeout ), id )

        val requestJson = Mapper.marshal( esReq )

        try {
            val body = RequestBody.create( requestJson, JSON )
            val request : Request = new Request.Builder().url( continueScrollUrl ).post( body ).build()
            val response : Response = okHttpClient.newCall( request ).execute()

            response.code() match {
                // deserialize json (response.body.string()) into DuplicateProbability object using Jackson mapper
                case 200 =>
                    val resJson = response.body.string()
                    val esRes = Mapper.unmarshal( resJson, classOf[ EsSearchResponse ] )

                    conv.esResponseToCorpexResults( esRes, corpexRequest )

                case _ =>
                    LOG.error( s"Error executing remote REST request : ${response.code} : ${response.body.string}" )
                    throw new Exception( s"Elasticsearch error\n${response.code} : ${response.body.string}" )
            }
        } catch {
            case e : IOException =>
                LOG.error( s"Exception caught trying to communicate with service: ${e.getMessage} : ${e.getCause}" )
                throw new Exception( s"Error submitting Elasticsearch request\n${e.getMessage} : ${e.getCause}" )
        }
    }

    private def scrollSearchEnd( id : String ) : Boolean = {
        val endScrollUrl = s"http://$esHost:$esPort/_search/scroll"
        val queryObj = EsScrollRequest( scrollId = id )

        val requestJson = Mapper.marshal( queryObj )
        LOG.info( "New ES request:" )
        LOG.info( requestJson )

        try {
            val body = RequestBody.create( requestJson, JSON )
            val request : Request = new Request.Builder().url( endScrollUrl ).delete( body ).build()
            val response : Response = okHttpClient.newCall( request ).execute()

            response.code() match {
                // deserialize json (response.body.string()) into DuplicateProbability object using Jackson mapper
                case 200 =>
                    true

                case _ =>
                    LOG.error( s"Elasticsearch error\n${response.code} : ${response.body.string}" )
                    false
            }
        } catch {
            case e : IOException =>
                LOG.error( s"Error submitting Elasticsearch request\n${e.getMessage} : ${e.getCause}" )
                false
        }
    }

    private def mapEsResults( results : CorpexSearchResults, mmap : scala.collection.mutable.Map[ String, Double ] ) : Unit = {
        if ( results.results.isDefined ) results.results.get.foreach( res => {
            val docId = res.cdr.documentId
            val score = res.esScore.getOrElse( -1.0 )
            mmap( docId ) = score
        } )
    }

    // Searches, but returns a map of all doc-ids to their ES score
    def searchGetAll( req : CorpexSearchRequest ) : Map[ String, Double ] = {

        var mutableMap = scala.collection.mutable.Map[ String, Double ]()

        val scrollReq = req.copy( pageSize = Some( scrollSize ) )

        val scrollSearchRes = scrollSearchStart( scrollReq )

        var searchRes = scrollSearchRes._1
        val scrollId = scrollSearchRes._2

        mapEsResults( searchRes, mutableMap )

        while ( searchRes.results.nonEmpty ) {
            searchRes = scrollSearchContinue( scrollId, req )

            mapEsResults( searchRes, mutableMap )
        }

        if ( !scrollSearchEnd( scrollId ) ) LOG.warn( s"Unable to close scroll: ${scrollId}" )

        LOG.info( s"Map size: ${mutableMap.size}" )
        LOG.info( s"Num results: ${scrollSearchRes._1.numResults}" )


        mutableMap.toMap
    }


    def serviceCheck : Future[ (Boolean, Option[ String ]) ] = Future {
        val countRes : CorpexSearchResults = count( CorpexSearchRequest( queries = Some( Nil ) ) )
        if (countRes.numResults == 0) (true, Some( "No documents in datastore" ) )
        else (true, None)
    } recover {
        case e : Throwable =>
            LOG.info( s"${e.getClass} -- ${e.getMessage}" )
            e.printStackTrace()
            (false, Some( "Unable to connect with search engine" ))
    }

}
