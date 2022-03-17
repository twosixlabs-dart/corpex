package com.twosixlabs.dart.corpex.api.tools

import com.fasterxml.jackson.databind.{JsonMappingException, ObjectMapper}
import com.fasterxml.jackson.databind.exc.{InvalidDefinitionException, InvalidTypeIdException, MismatchedInputException, UnrecognizedPropertyException}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.twosixlabs.dart.corpex.api.exceptions.InvalidRequestException

import scala.util.{Failure, Success, Try}

object Mapper {
    private val m : ObjectMapper = {
        val mapper = new ObjectMapper ()
        mapper registerModule (DefaultScalaModule)
        mapper registerModule( new JavaTimeModule )
        mapper
    }

    private val invalidQueryTypePattern = raw"(?s).*Could not resolve type id '(.+)' as a subtype of.*".r
    private val missingQueryTypePattern = raw"(?s).*Missing type id when trying to resolve subtype of.*".r
    private val unknownFieldPattern = raw"""(?s).*Unrecognized field (".+") .*""".r
    private val jsonLocationPattern = raw"(?s).*at \[Source: .*; (line: [0-9]+, column: [0-9]+).*".r

    def unmarshal[A](json: String, valueType: Class[A] ) : A =
        Try( m.readValue( json, valueType ) ) match {
            case Success( res ) => res
            case Failure( e : InvalidTypeIdException )  => e.getMessage match {
                case invalidQueryTypePattern( badQueryType : String ) => throw new InvalidRequestException( s"invalid query type: ${badQueryType}" ) {
                    override def getCause : Throwable = e
                }

                case missingQueryTypePattern() => throw new InvalidRequestException( "missing query_type or agg_type" ) {
                    override def getCause : Throwable = e
                }
            }

            case Failure( e : InvalidDefinitionException ) => throw e.getCause

            case Failure( e : UnrecognizedPropertyException ) => e.getMessage match {
                case unknownFieldPattern( field : String ) => throw new InvalidRequestException( s"invalid field: ${field}" )
                case _ => throw new InvalidRequestException( "invalid field" )
            }

            case Failure( e : MismatchedInputException ) => e.getMessage match {
                case jsonLocationPattern( jsonLoc ) => throw new InvalidRequestException( s"invalid field type at ${jsonLoc}" ) {
                    override def getCause : Throwable = e
                }

                case _ => throw new InvalidRequestException( "invalid field type" ) {
                    override def getCause : Throwable = e
                }
            }

            case Failure( e : JsonMappingException ) => e.getMessage match {
                case jsonLocationPattern( jsonLoc : String ) => throw new InvalidRequestException( s"unable to parse query json at ${jsonLoc}" ) {
                    override def getCause : Throwable = e
                }

                case _ => throw new InvalidRequestException( s"unable to parse query json" ) {
                    override def getCause : Throwable = e
                }
            }

            case Failure( e : Throwable ) => throw new InvalidRequestException( s"unknown error parsing query json" ) {
                override def getCause : Throwable = e
            }
        }

    def marshal(dto: Any) : String = m.writeValueAsString( dto )
}
