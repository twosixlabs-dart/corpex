package com.twosixlabs.dart.corpex.api.configuration.annotations

import com.twosixlabs.dart.corpex.api.configuration.ConfigEnum

sealed case class TagType( cdrLabel : String,
                           apiLabel : String,
                           humanLabel : Option[ String ] = None,
                           description : Option[ String ] = None )

object EventTagTypes extends ConfigEnum[ TagType ] {
    def genTag( tag : String, humanLabel : String, description : String = "" ) =
        TagType( tag, tag, Some( humanLabel ), if (description == "") None else Some( description ) )

    val B_ACTION = V( TagType( "B-action", "B-action", Some( "B-action" ) ) )
    val NIL = V( genTag( "NIL", "Uncategorized" ) )
    val CHANGE = V( genTag( "Change", "Change" ) )
    val EXPECT = V( genTag( "Expect", "Expectation" ) )
    val FIND = V( genTag( "Find", "Discovery" ) )
    val RECEIVE = V( genTag( "Receive", "Receiving" ) )
    val MOVE = V( genTag( "Move", "Moving" ) )
    val USE = V( genTag( "Use", "Usage" ) )
    val OBSERVE = V( genTag( "Observe", "Observation" ) )
    val NEED = V( genTag( "Need", "Need" ) )
    val MAKE = V( genTag( "Make", "Making" ) )
    val INCREASING = V( genTag( "Increasing", "Increase" ) )
    val DECREASING = V( genTag( "Decreasing", "Decrease" ) )
    val CAUSE = V( genTag( "Cause", "Causation" ) )
    val CONFLICT = V( genTag( "Conflict", "Conflict" ) )
    val VIOLENCE = V( genTag( "Violence", "Violence" ) )
    val LIFE_EVENT = V( genTag( "LifeEvent", "Life Event" ) )
    val COMMUNICATION = V( genTag( "Communication", "Communication" ) )
    val TRANSPORT = V( genTag( "Transport", "Transport" ) )
    val FOOD_INSECURITY = V( genTag( "FoodInsecurity", "Food Insecurity" ) )
    val JUSTICE = V( genTag( "Justice", "Justice" ) )
    val CONTACT = V( genTag( "Contact", "Contact" ) )
    val PERSONNEL = V( genTag( "Personnel", "Personnel" ) )
    val TRADE = V( genTag( "Trade", "Trade" ) )
    val HELP = V( genTag( "Help", "Aid" ) )
    val AGRICULTURE = V( genTag( "Agriculture", "Agriculture" ) )
    val MANUFACTURING = V( genTag( "Manufacturing", "Manufacturing" ) )
    val RAW_MATERIALS = V( genTag( "RawMaterials", "Raw Materials" ) )
    val HEALTH_DISEASE = V( genTag( "HealthDisease", "Health/Disease" ) )
    val THEFT = V( genTag( "Theft", "Theft" ) )
    val WEATHER = V( genTag( "Weather", "Weather" ) )
    val TRANSPORTATION = V( genTag( "Transportation", "Transportation" ) )
    val POS_EMOTIONS = V( genTag( "PosEmotions", "Positive Emotion" ) )
    val NEG_EMOTIONS = V( genTag( "NegEmotions", "Negative Emotion" ) )
    val CALCULATE = V( genTag( "Calculate", "Calculation" ) )
    val BEGIN = V( genTag( "Begin", "Beginning" ) )
    val SELECT = V( genTag( "Select", "Selection" ) )
    val PREPARE = V( genTag( "Prepare", "Preparation" ) )
    val AVAILABLE = V( genTag( "Available", "Availability" ) )
}

object EntityTagTypes extends ConfigEnum[ TagType ] {
    def genTag( tag : String, humanLabel : String, description : String = "" ) =
        TagType( tag, tag, Some( humanLabel ), if (description == "") None else Some( description ) )

    val PERSON = V( genTag( "PERSON", "Person", "People, including fictional" ) )
    val NORP = V( genTag( "NORP", "Nationality/Identity", "Nationalities or religious or political groups" ) )
    val FAC = V( genTag( "FAC", "Facility", "Buildings, airports, highways, bridges, etc" ) )
    val ORG = V( genTag( "ORG", "Organization", "Companies, agencies, institutions, etc" ) )
    val GPE = V( genTag( "GPE", "Geo-Political Entity", "Countries, cities, states." ) )
    val LOC = V( genTag( "LOC", "Location", "Non-GPE locations, mountain ranges, bodies of water." ) )
    val PRODUCT = V( genTag( "PRODUCT", "Tangible Product", "Objects, vehicles, foods, etc. (Not services)." ) )
    val EVENT = V( genTag( "EVENT", "Event", "Named hurricanes, battles, wars, sports events, etc." ) )
    val WORK_OF_ART = V( genTag( "WORK_OF_ART", "Work of Art", "Titles of books, songs, etc." ) )
    val LAW = V( genTag( "LAW", "Law", "Named documents made into laws." ) )
    val LANGUAGE = V( genTag( "LANGUAGE", "Language", "Any named language." ) )
    val DATE = V( genTag( "DATE", "Date", "Absolute or relative dates or periods." ) )
    val TIME = V( genTag( "TIME", "Time", "Times smaller than a day." ) )
    val PERCENT = V( genTag( "PERCENT", "Percent", "Percentage, including “%“." ) )
    val MONEY = V( genTag( "MONEY", "Money", "Monetary values, including unit." ) )
    val QUANTITY = V( genTag( "QUANTITY", "Quantity", "Measurements, as of weight or distance." ) )
    val ORDINAL = V( genTag( "ORDINAL", "Ordinal Number", "“first”, “second”, etc." ) )
    val CARDINAL = V( genTag( "CARDINAL", "Cardinal Number", "Numerals that do not fall under another type." ) )
}

object AllTags extends ConfigEnum[ TagType ] {
    V( EventTagTypes.values )
    V( EntityTagTypes.values )
}
