package implicitly

import com.mongodb.casbah._

object Store {
  def withDb[T](name: String)(f: MongoDB => T):T = {
     val conn = MongoConnection(Props.get("mongo.host"), Props.getInt("mongo.port"))
     val db = conn(name)
     db.authenticate(Props.get("mongo.user"), Props.get("mongo.passwd"))
     try { f(db) }
     finally { conn.close() }
  }

  def collection[T](name: String)(f: MongoCollection => T):T =
     withDb(Props.get("mongo.db")) { db =>
        f(db(name))
     }
}

object Libraries {
  import com.mongodb.casbah.commons.MongoDBObject
  import com.mongodb.casbah.Imports._

  implicit val l2m: Library => DBObject = (l: Library) =>
    MongoDBObject(
      "organization" -> l.organization,
      "name" -> l.name,
      "version" -> l.version,
      "description" -> l.description,
      "tags" -> l.tags,
      "docs" -> l.docs,
      "resolvers" -> l.resolvers,
      "dependencies" -> MongoDBObject(
         "projects" -> l.dependencies.projects.map { p =>
           MongoDBObject(
             "organization" -> p.organization,
             "name" -> p.name,
             "version" -> p.version
           )
         },
         "libraries" -> l.dependencies.libraries.map { dl =>
           MongoDBObject(
             "organization" -> dl.organization,
             "name" -> dl.name,
             "version" -> dl.version
           )
         }
       ),
      "ghuser" -> l.ghuser,
      "ghrepo" -> l.ghrepo
     )

  private def innerList(m: MongoDBObject)(objName: String, lname: String) =
     wrapDBList(wrapDBObj(m.getAs[BasicDBList](objName).get).getAs[BasicDBList](lname).get)

  // todo: this smells boiler platey, think about pimping this a bit more...
  implicit val m2l: MongoDBObject => Library = (m) => Library(
    m.getAs[String]("organization").get, m.getAs[String]("name").get, m.getAs[String]("version").get,
    m.getAs[String]("description").get, wrapDBList(m.getAs[BasicDBList]("tags").get).iterator.map(_.toString).toSeq, m.getAs[String]("docs").get,
    wrapDBList(m.getAs[BasicDBList]("resolvers").get).iterator.map(_.toString).toSeq, Dependencies(
     innerList(m)("dependencies", "projects").iterator.map(p => {
       val mo = wrapDBObj(p.asInstanceOf[DBObject])
       ModuleID(mo.getAs[String]("organization").get, mo.getAs[String]("name").get, mo.getAs[String]("version").get)
     }).toSeq,
     innerList(m)("dependencies", "libraries").iterator.map(l => {
      val mo = wrapDBObj(l.asInstanceOf[DBObject])
      ModuleID(mo.getAs[String]("organization").get, mo.getAs[String]("name").get, mo.getAs[String]("version").get)
     }).toSeq
    ), m.getAs[String]("ghuser"), m.getAs[String]("ghrepo")
  )

  implicit val mc2l: MongoCollection => Iterable[Library] = (m) => for(l <- m) yield m2l(l)

  def all[T](f: Iterable[Library] => T)(implicit m2s: MongoCollection => Iterable[Library]) = Store.collection("libraries") { c => f(m2s(c)) }

  def save(libs: Seq[Library]) =
    Store.collection("libraries") { col => libs foreach(col += _) }
}
