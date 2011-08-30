package ls

import com.mongodb.casbah._

object Store {
  def withDb[T](name: String)(f: MongoDB => T): T = {
     val conn = MongoConnection(Props.get("mongo.host"), Props.getInt("mongo.port"))
     val db = conn(name)
     db.authenticate(Props.get("mongo.user"), Props.get("mongo.passwd"))
     f(db)
  }

  def collection[T](name: String)(f: MongoCollection => T): T =
     withDb(Props.get("mongo.db")) { db =>
        f(db(name))
     }
}
