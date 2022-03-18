package com.twosixlabs.dart.corpex.services.es.models

import com.twosixlabs.dart.corpex.api.enums.SortType
import com.twosixlabs.dart.corpex.api.tools.Mapper
import com.twosixlabs.dart.corpex.services.search.es.models.EsSortKey
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class EsSortKeyTest extends AnyFlatSpecLike with Matchers {

    behavior of "EsSortKey"

    it should "Serialize a field-only key to a string" in {
        val key = EsSortKey( "test", None )

        val serialized = Mapper.marshal( key )
        serialized shouldBe """"test""""
    }

    it should "Serialize a key with a field and a sort direction as an object" in {
        val key = EsSortKey( "test", Some( SortType.ASC ) )

        val serialized = Mapper.marshal( key )
        serialized shouldBe """{"test":"asc"}"""
    }

    it should "Serialize an array of keys properly" in {
        val keys = List( EsSortKey( "test_1", None ),
                         EsSortKey( "test_2", Some( SortType.ASC ) ),
                         EsSortKey( "test_3", None ),
                         EsSortKey( "test_4", Some( SortType.DESC ) ) )

        val serialized = Mapper.marshal( keys )
        serialized shouldBe """["test_1",{"test_2":"asc"},"test_3",{"test_4":"desc"}]"""
    }

}
