package com.twosixlabs.dart.corpex.api.models

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonMappingException, JsonNode, JsonSerializer, SerializerProvider}

@JsonDeserialize( using = classOf[ MultiValueDeserializer ] )
@JsonSerialize( using = classOf[ MultiValueSerializer ] )
case class MultiValue( long : Option[ Long ] = None, string : Option[ String ] = None, double : Option[ Double ] = None ) {
    if ( long.isDefined && ( string.isDefined || double.isDefined ) ) throw new IllegalStateException( "MultiValue can have only one value" )
    if ( string.isDefined && double.isDefined ) throw new IllegalStateException( "MultiValue can have only one value" )

    def isEmpty : Boolean = long.isEmpty && string.isEmpty && double.isEmpty
    def isDefined : Boolean = !isEmpty

    def get() : Any = {
        if ( long.isDefined ) long.get
        else if ( string.isDefined ) string.get
        else if ( double.isDefined ) double.get
        else None.get
    }

    def getOrElse( value : Any ) : Any = {
        if ( isDefined ) get( )
        else value
    }

    def map[T]( fn : Any => T ) : Option[ T ] = {
        if ( this.isEmpty ) None
        else Some( fn( this.get() ) )
    }
}

class MultiValueDeserializer extends JsonDeserializer[ MultiValue ] {
    override def deserialize( p : JsonParser, ctxt : DeserializationContext ) : MultiValue = {
        val node : JsonNode = p.getCodec.readTree( p )
        if ( node.isInt ) MultiValue( long = Some( node.intValue.toLong ) )
        else if ( node.isLong || node.isInt ) MultiValue( long = Some( node.longValue ) )
        else if ( node.isDouble ) MultiValue( double = Some( node.doubleValue ) )
        else if ( node.isFloat ) MultiValue( double = Some( node.floatValue.toDouble ) )
        else if ( node.isTextual ) MultiValue( string = Some( node.asText ) )
        else if ( node.isNull ) MultiValue()
        else throw new JsonMappingException( "Cannot deserialize to MultiValue: node must be integer/long, float/double, or string" )
    }

    override def getNullValue() : MultiValue = MultiValue()
}

class MultiValueSerializer extends JsonSerializer[ MultiValue ] {
    override def serialize( value : MultiValue, gen : JsonGenerator, serializers : SerializerProvider ) : Unit = {
        value match {
            case MultiValue( Some( long : Long ), None, None ) => gen.writeNumber( long )
            case MultiValue( None, None, Some( double : Double ) ) => gen.writeNumber( double )
            case MultiValue( None, Some( string : String ), None ) => gen.writeString( string )
            case MultiValue( None, None, None ) => gen.writeNull()
            case _ => throw new IllegalStateException( "MultiValue cannot have multiple types defined" )
        }
    }
}
