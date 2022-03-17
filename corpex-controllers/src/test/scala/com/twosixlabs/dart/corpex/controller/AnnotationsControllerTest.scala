package com.twosixlabs.dart.corpex.controller

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatra.test.scalatest.ScalatraSuite
import org.slf4j.{Logger, LoggerFactory}

class AnnotationsControllerTest extends AnyFlatSpecLike with ScalatraSuite with Matchers {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    val config : Config = ConfigFactory.load( "test" ).resolve()

    val baseDependencies = SecureDartController.deps( "corpex", config )

    addServlet( new AnnotationsController( baseDependencies ), "/*" )

    "GET from /tags" should "return 200 and a complete list of tag profiles" in {
        get( "/tags" ) {
            status shouldBe 200
            response.body shouldBe """[{"tag_id":"qntfy-event","cdr_label":"qntfy-events-annotator","label":"Qntfy Events","description":"Detected events extractions"},{"tag_id":"qntfy-ner","cdr_label":"qntfy-ner-annotator","label":"Qntfy NER","description":"Named entity extractions"}]"""
        }
    }

    "GET from /tags/qntfy-ner/types" should "return 200 and a complete list of entity types" in {
        get( "/tags/qntfy-ner/types" ) {
            response.getContentType() shouldBe "application/json;charset=utf-8"
            status shouldBe 200
            response.body shouldBe
            """[{"tag_type":"CARDINAL","cdr_label":"CARDINAL","label":"Cardinal Number","description":"Numerals that do not fall under another type."},{"tag_type":"DATE","cdr_label":"DATE","label":"Date","description":"Absolute or relative dates or periods."},{"tag_type":"EVENT","cdr_label":"EVENT","label":"Event","description":"Named hurricanes, battles, wars, sports events, etc."},{"tag_type":"FAC","cdr_label":"FAC","label":"Facility","description":"Buildings, airports, highways, bridges, etc"},{"tag_type":"GPE","cdr_label":"GPE","label":"Geo-Political Entity","description":"Countries, cities, states."},{"tag_type":"LANGUAGE","cdr_label":"LANGUAGE","label":"Language","description":"Any named language."},{"tag_type":"LAW","cdr_label":"LAW","label":"Law","description":"Named documents made into laws."},{"tag_type":"LOC","cdr_label":"LOC","label":"Location","description":"Non-GPE locations, mountain ranges, bodies of water."},{"tag_type":"MONEY","cdr_label":"MONEY","label":"Money","description":"Monetary values, including unit."},{"tag_type":"NORP","cdr_label":"NORP","label":"Nationality/Identity","description":"Nationalities or religious or political groups"},{"tag_type":"ORDINAL","cdr_label":"ORDINAL","label":"Ordinal Number","description":"“first”, “second”, etc."},{"tag_type":"ORG","cdr_label":"ORG","label":"Organization","description":"Companies, agencies, institutions, etc"},{"tag_type":"PERCENT","cdr_label":"PERCENT","label":"Percent","description":"Percentage, including “%“."},{"tag_type":"PERSON","cdr_label":"PERSON","label":"Person","description":"People, including fictional"},{"tag_type":"PRODUCT","cdr_label":"PRODUCT","label":"Tangible Product","description":"Objects, vehicles, foods, etc. (Not services)."},{"tag_type":"QUANTITY","cdr_label":"QUANTITY","label":"Quantity","description":"Measurements, as of weight or distance."},{"tag_type":"TIME","cdr_label":"TIME","label":"Time","description":"Times smaller than a day."},{"tag_type":"WORK_OF_ART","cdr_label":"WORK_OF_ART","label":"Work of Art","description":"Titles of books, songs, etc."}]"""
        }
    }

    "GET from /tags/qntfy-event/types" should "return 200 and a complete list of event types" in {
        get( "/tags/qntfy-event/types" ) {
            status shouldBe 200
            response.body shouldBe """[{"tag_type":"Agriculture","cdr_label":"Agriculture","label":"Agriculture"},{"tag_type":"Available","cdr_label":"Available","label":"Availability"},{"tag_type":"B-action","cdr_label":"B-action","label":"B-action"},{"tag_type":"Begin","cdr_label":"Begin","label":"Beginning"},{"tag_type":"Calculate","cdr_label":"Calculate","label":"Calculation"},{"tag_type":"Cause","cdr_label":"Cause","label":"Causation"},{"tag_type":"Change","cdr_label":"Change","label":"Change"},{"tag_type":"Communication","cdr_label":"Communication","label":"Communication"},{"tag_type":"Conflict","cdr_label":"Conflict","label":"Conflict"},{"tag_type":"Contact","cdr_label":"Contact","label":"Contact"},{"tag_type":"Decreasing","cdr_label":"Decreasing","label":"Decrease"},{"tag_type":"Expect","cdr_label":"Expect","label":"Expectation"},{"tag_type":"Find","cdr_label":"Find","label":"Discovery"},{"tag_type":"FoodInsecurity","cdr_label":"FoodInsecurity","label":"Food Insecurity"},{"tag_type":"HealthDisease","cdr_label":"HealthDisease","label":"Health/Disease"},{"tag_type":"Help","cdr_label":"Help","label":"Aid"},{"tag_type":"Increasing","cdr_label":"Increasing","label":"Increase"},{"tag_type":"Justice","cdr_label":"Justice","label":"Justice"},{"tag_type":"LifeEvent","cdr_label":"LifeEvent","label":"Life Event"},{"tag_type":"Make","cdr_label":"Make","label":"Making"},{"tag_type":"Manufacturing","cdr_label":"Manufacturing","label":"Manufacturing"},{"tag_type":"Move","cdr_label":"Move","label":"Moving"},{"tag_type":"NIL","cdr_label":"NIL","label":"Uncategorized"},{"tag_type":"Need","cdr_label":"Need","label":"Need"},{"tag_type":"NegEmotions","cdr_label":"NegEmotions","label":"Negative Emotion"},{"tag_type":"Observe","cdr_label":"Observe","label":"Observation"},{"tag_type":"Personnel","cdr_label":"Personnel","label":"Personnel"},{"tag_type":"PosEmotions","cdr_label":"PosEmotions","label":"Positive Emotion"},{"tag_type":"Prepare","cdr_label":"Prepare","label":"Preparation"},{"tag_type":"RawMaterials","cdr_label":"RawMaterials","label":"Raw Materials"},{"tag_type":"Receive","cdr_label":"Receive","label":"Receiving"},{"tag_type":"Select","cdr_label":"Select","label":"Selection"},{"tag_type":"Theft","cdr_label":"Theft","label":"Theft"},{"tag_type":"Trade","cdr_label":"Trade","label":"Trade"},{"tag_type":"Transport","cdr_label":"Transport","label":"Transport"},{"tag_type":"Transportation","cdr_label":"Transportation","label":"Transportation"},{"tag_type":"Use","cdr_label":"Use","label":"Usage"},{"tag_type":"Violence","cdr_label":"Violence","label":"Violence"},{"tag_type":"Weather","cdr_label":"Weather","label":"Weather"}]"""
        }
    }

    "GET from /facets" should "return list of facet ids" in {
        get( "/facets" ) {
            status shouldBe 200
            response.body shouldBe """[{"facet_id":"factiva-industry","has_score":false,"cdr_label":"factiva-industries","label":"Factiva Industries","description":"Industries identified by Dow Jones' Factiva service (Factiva docs only"},{"facet_id":"factiva-region","has_score":false,"cdr_label":"factiva-regions","label":"Factiva Regions","description":"Contextual regions identified by Dow Jones' Factiva service (Factiva docs only)"},{"facet_id":"factiva-subject","has_score":false,"cdr_label":"factiva-subjects","label":"Factiva Subjects","description":"Subjects identified by Dow Jones' Factiva service (Factiva docs only"},{"facet_id":"qntfy-sentiment","has_score":true,"cdr_label":"qntfy-sentiment-annotator","label":"Qntfy Sentiment/Subjectivity","description":"Bias and stance detection"},{"facet_id":"qntfy-topic","has_score":true,"cdr_label":"qntfy-categories-annotator","label":"Qntfy Topics","description":"Predicted topics/categories with confidence scores"}]"""
        }
    }

    "GET from /facets/:facet_id" should "return 200 and an a json object with has_score = true or false" in {
        get( "/facets/qntfy-topic" ) {
            status shouldBe 200
            response.body shouldBe """{"facet_id":"qntfy-topic","has_score":true,"cdr_label":"qntfy-categories-annotator","label":"Qntfy Topics","description":"Predicted topics/categories with confidence scores"}"""
        }

        get( "/facets/factiva-region" ) {
            status shouldBe 200
            response.body shouldBe """{"facet_id":"factiva-region","has_score":false,"cdr_label":"factiva-regions","label":"Factiva Regions","description":"Contextual regions identified by Dow Jones' Factiva service (Factiva docs only)"}"""
        }
    }

    "GET from /facets/:facet_id" should "return 404 when :facet_id does not exist" in {
        get( "/facets/bad-id" ) {
            status shouldBe 404
            response.body shouldBe """{"status":404,"error_message":"Resource not found: facet id \"bad-id\" does not exist"}"""
        }
    }

    override def header = null
}
