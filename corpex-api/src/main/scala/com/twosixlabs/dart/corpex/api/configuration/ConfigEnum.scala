package com.twosixlabs.dart.corpex.api.configuration

trait ConfigEnum[ T ] {
    private val vals : scala.collection.mutable.Set[ T ] = scala.collection.mutable.Set[ T ]()

    def V( t : T ) : T = {
        vals.add( t )
        t
    }

    def V( tSet : Set[ T ] ) : Unit = vals ++= tSet

    def values : Set[ T ] = vals.toSet

    def check( value : T ) : Boolean = vals contains value

    def check[A]( value : A, transform : T => A ) : Boolean = vals.map( transform( _ ) ).contains( value )

    def get[A]( value : A, transform : T => A ) : Option[ T ] = vals.find( transform( _ ) == value )
}
