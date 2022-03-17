package com.twosixlabs.dart.corpex.api.configuration

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

object TestConfigEnumString extends ConfigEnum[ String ] {
    val FIRST_CASE = V("test value")
    val SECOND_CASE = V("second test value")
    val THIRD_CASE = V("another value (test)")
}

case class TestCase( valOne : String, valTwo : Int )

object TestConfigEnum extends ConfigEnum[ TestCase ] {
    val FIRST_CASE = V(TestCase( "test value", 1 ))
    val SECOND_CASE = V(TestCase( "another value", 2 ))
    val THIRD_CASE = V(TestCase( "third value", 3 ))
}


class ConfigEnumTest extends AnyFlatSpecLike with Matchers {

    "ConfigEnum" should "validate an object against is members" in {
        val caseOne = TestCase( "test value", 1 )
        TestConfigEnum.check( caseOne ) shouldBe true
        caseOne shouldBe TestConfigEnum.FIRST_CASE

        val caseTwo = TestCase( "made up value", 99 )
        TestConfigEnum.check( caseTwo ) shouldBe false

        val caseThree = TestCase( "third value", 2 )
        TestConfigEnum.check( caseThree ) shouldBe false
    }

    "ConfigEnum" should "validate an object using just one field or a transform of multiple fields" in {
        TestConfigEnum.check( "test value", _.valOne ) shouldBe true
        TestConfigEnum.check( "made up value", _.valOne ) shouldBe false
        TestConfigEnum.check( "third value", _.valOne ) shouldBe true
        TestConfigEnum.check( 2, _.valTwo ) shouldBe true
        TestConfigEnum.check( 10, _.valTwo ) shouldBe false
        TestConfigEnum.check( "another value 2", v => s"${v.valOne} ${v.valTwo}" ) shouldBe true
        TestConfigEnum.check( "test value 3", v => s"${v.valOne} ${v.valTwo}" ) shouldBe false
    }

    "ConfigEnum" should "retrieve an object using one field or a transform of multiple fields" in {
        val retrievedOne = TestConfigEnum.get( "test value", _.valOne )
        retrievedOne.get shouldBe TestConfigEnum.FIRST_CASE

        val retrievedTwo = TestConfigEnum.get( 2, _.valTwo )
        retrievedTwo.get shouldBe TestConfigEnum.SECOND_CASE

        val retrievedThree = TestConfigEnum.get( "3 third value", v => s"${v.valTwo} ${v.valOne}" )
        retrievedThree.get shouldBe TestConfigEnum.THIRD_CASE

        val retrievedFour = TestConfigEnum.get( 12, v => v.valOne.length + v.valTwo.toString.length )
        retrievedFour.get shouldBe TestConfigEnum.THIRD_CASE
    }

    "ConfigEnum" should "return None when getting with non-existent values" in {
        TestConfigEnum.get( "non existent value", _.valOne) shouldBe None
        TestConfigEnum.get( 8, _.valTwo) shouldBe None
        TestConfigEnum.get( 18, v => v.valOne.length + v.valTwo.toString.length ) shouldBe None
    }

}
