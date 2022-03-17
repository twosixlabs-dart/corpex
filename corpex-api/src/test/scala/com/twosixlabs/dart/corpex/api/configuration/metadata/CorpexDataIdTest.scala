package com.twosixlabs.dart.corpex.api.configuration.metadata

import org.scalatest.{FlatSpecLike, Matchers}

class CorpexDataIdTest extends FlatSpecLike with Matchers {

    behavior of "DataId.check"

    it should "return true when DataId exists" in {
        val isItThere : Boolean = DataIds.check( "cdr.extracted_metadata.Title", _.apiLabel )

        isItThere shouldBe true
    }

    it should "return false when DataId doesn't exist" in {
        val isItThere : Boolean = DataIds.check( "nonExistentField", _.apiLabel )

        isItThere shouldBe false
    }

    behavior of "DataId.get"

    it should "retrieve DataId from apiLabel" in {
        val dataId : Option[ DataId ] = DataIds.get( "cdr.extracted_metadata.Title", _.apiLabel )

        dataId.get.cdrLabel.get shouldBe "extracted_metadata.Title"
    }

    it should "retrieve DataId from cdrLabel" in {
        val dataId : Option[ DataId ] = DataIds.get( "extracted_metadata.Title", _.cdrLabel.get )

        dataId.get.apiLabel shouldBe "cdr.extracted_metadata.Title"
    }

    it should "return None when id doesn't exist" in {
        val dataId : Option[ DataId ] = DataIds.get( "cdr.NonExistantField", _.apiLabel )

        dataId shouldBe None
    }

}
