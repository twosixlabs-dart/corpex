package com.twosixlabs.dart.corpex.api.models

import com.twosixlabs.dart.corpex.api.tools.Mapper
import org.scalatest.{FlatSpecLike, Matchers}

class MultiValueTest extends FlatSpecLike with Matchers {

    behavior of "MultiValue"

    it should "unmarshal a long" in {
        val json = "12341234"

        val value = Mapper.unmarshal( json, classOf[ MultiValue ] )
        value.long shouldBe Some( 12341234L )
        value.double shouldBe None
        value.string shouldBe None
        value.isEmpty shouldBe false
        value.isDefined shouldBe true
    }

    it should "marshal a long" in {
        val value = MultiValue( long = Some( 12341234L ) )

        val json = Mapper.marshal( value )
        json shouldBe "12341234"
    }

    it should "unmarshal a double" in {
        val json = "0.23423"

        val value = Mapper.unmarshal( json, classOf[ MultiValue ] )
        value.long shouldBe None
        value.double shouldBe Some( 0.23423 )
        value.string shouldBe None
        value.isEmpty shouldBe false
        value.isDefined shouldBe true
    }

    it should "marshal a double" in {
        val value = MultiValue( double = Some( 0.23423 ) )

        val json = Mapper.marshal( value )
        json shouldBe "0.23423"
    }

    it should "unmarshal a string" in {
        val json = "\"some string value\""

        val value = Mapper.unmarshal( json, classOf[ MultiValue ] )
        value.long shouldBe None
        value.double shouldBe None
        value.string shouldBe Some( "some string value" )
        value.isEmpty shouldBe false
        value.isDefined shouldBe true
    }

    it should "marshal a string" in {
        val value = MultiValue( string = Some( "some string value" ) )

        val json = Mapper.marshal( value )
        json shouldBe "\"some string value\""
    }

    it should "unmarshal a null value" in {
        val json = "null"

        val value = Mapper.unmarshal( json, classOf[ MultiValue ] )
        value.long shouldBe None
        value.double shouldBe None
        value.string shouldBe None
        value.isEmpty shouldBe true
        value.isDefined shouldBe false
    }

    it should "marshal a null value" in {
        val value = MultiValue()

        val json = Mapper.marshal( value )
        json shouldBe "null"
    }
}
