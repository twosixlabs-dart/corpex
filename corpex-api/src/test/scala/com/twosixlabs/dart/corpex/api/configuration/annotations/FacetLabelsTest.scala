package com.twosixlabs.dart.corpex.api.configuration.annotations

import org.scalatest.{FlatSpecLike, Matchers}

class FacetLabelsTest extends FlatSpecLike with Matchers {

    "FacetIds.fromCdr" should "get a FacetId object from a cdr label" in {
        val facetId = FacetIds.fromCdr("qntfy-categories-annotator")
        facetId shouldBe FacetIds.QNTFY_TOPIC
    }

    "FacetIds.fromApi" should "get a FacetId object using an api label" in {
        val facetId = FacetIds.fromApi("factiva-region")
        facetId shouldBe FacetIds.FACTIVA_REGION
    }

}
