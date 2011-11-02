package ls

import com.mongodb.casbah._
import com.mongodb.MongoURI
import java.net.URI

object Store extends Logged {
  // casbahs underlying java driver supports connection 
  // pooling out of the box, we should only need one instance
  // for entire app instance
  // http://www.mongodb.org/display/DOCS/Java+Driver+Concurrency

  lazy val db = {
    // todo: using the heroku specific config property was easy,
    //  make it elegant
    val uri = new URI(Props.get("MONGOLAB_URI"))
    try {
      val conn = MongoConnection(uri.getHost, uri.getPort)
      val name = uri.getPath.drop(1)
      val mongo = Clock("connecting to %s %s" format(uri, name), log) {
        conn(name)
      }
      Clock("authenticating", log) {
        val Array(user, pass) = uri.getUserInfo.split(":")
        mongo.authenticate(user, pass)
        mongo
      }
    } catch {
      case e:java.io.IOException => log.error(
        "Error occured whilst connecting to mongo (%s): %s" format(
          uri, e.getMessage), Some(e)
      )
      throw e
    }
  }

  def collection[T](name: String)(f: MongoCollection => T): T = f(db(name))
}
