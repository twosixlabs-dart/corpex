import com.twosixlabs.dart.commons.config.{CliConfig, StandardCliConfig}
import com.typesafe.config.{Config, ConfigFactory}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.slf4j.{Logger, LoggerFactory}

object Main extends StandardCliConfig {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    def main( args : Array[ String ] ) : Unit = {

        loadEnvironment( args )

        val config : Config = ConfigFactory.defaultApplication().resolve()

        val port = config.getInt( "corpex.http.port" )
        val server = new Server( port )
        val context = new WebAppContext()

        context.setContextPath( "/" )
        context.setInitParameter( ScalatraListener.LifeCycleKey, "com.twosixlabs.dart.corpex.ScalatraInit" ) // scalatra uses some magic defaults I don't like
        context.addEventListener( new ScalatraListener )

        server.setHandler( context )
        server.start()
        server.join()

    }
}
