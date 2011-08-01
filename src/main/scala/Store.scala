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

  def all[T](f: MongoCollection => T) = Store.collection("libraries") { f }

  def save(libs: Seq[Library]) =
    Store.collection("libraries") { col => libs foreach(col += _) }
}
