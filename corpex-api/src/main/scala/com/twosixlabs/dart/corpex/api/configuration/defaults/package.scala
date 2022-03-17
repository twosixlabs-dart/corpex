package com.twosixlabs.dart.corpex.api

import com.twosixlabs.dart.corpex.api.enums.SortValues

package object defaults {
    val DEFAULT_SORT : SortValues.Value = SortValues.NUM_DOCS
    val DEFAULT_ANNOTATION_PAGE_SIZE = 500
    val DEFAULT_FACET_PAGE_SIZE = 500
    val DEFAULT_SEARCH_PAGE_SIZE = 50
}
