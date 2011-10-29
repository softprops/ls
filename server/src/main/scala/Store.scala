package ls

import com.mongodb.casbah._
import com.mongodb.MongoURI
import java.net.URI

object Store extends Logged {
  def withDb[T](name: Option[String] = None)(f: MongoDB => T): T = {
    // todo: using the heroku specific config property was easy,
    //  make it elegant
    val uri = new URI(Props.get("MONGOLAB_URI"))
    try {
      val conn = MongoConnection(uri.getHost, uri.getPort)
      val dbName = name match {
        case Some(db) => db
        case _ => uri.getPath.drop(1)
      }
      println("connecting to %s %s" format(uri, dbName))
      val db = conn(dbName)
      val Array(user, pass) = uri.getUserInfo.split(":")
      db.authenticate(user, pass)
      f(db)
    } catch {
      case e:java.io.IOException => log.error(
        "Error occured whilst connecting to mongo (%s): %s" format(
          uri, e.getMessage), Some(e)
      )
      throw e
    }
  }

  def collection[T](name: String, dbname: Option[String] = None)(
    f: MongoCollection => T): T =
      withDb(dbname) { db =>
        f(db(name))
      }
}
