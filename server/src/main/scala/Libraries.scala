package ls

case class ModuleID(organization: String, name: String, version: String)
case class Dependencies(libraries: Seq[ModuleID])
case class Library(organization: String, name: String, version: String,
                   description: String, site: String, tags: Seq[String],
                   docs: String, resolvers: Seq[String],
                   dependencies: Dependencies,
                   ghuser: Option[String] = None, ghrepo: Option[String] = None)


object Convertions {

}

object Libraries {
  import com.mongodb.casbah.commons.{MongoDBObject => Obj}
  import com.mongodb.casbah.{MongoCollection}
  import com.mongodb.{BasicDBList, DBObject}
  import com.mongodb.casbah.Implicits._

  type CanConvertListTo[A] = Iterator[DBObject] => Iterable[A]
  type CanConvertTo[A, B] = A => B

  val DefaultLimit = 20
  val Latest = "latest"

  private def libraries[T](f: MongoCollection => T) =
    Store.collection("libraries")(f)

  implicit val l2m: Library => DBObject = (l: Library) =>
    Obj(
      "organization" -> l.organization,
      "name" -> l.name,
      "version" -> l.version,
      "description" -> l.description,
      "_keywords" -> (l.description.replaceAll("""\s+"""," ").toLowerCase.split(" ") ++
         l.tags ++ Seq(l.organization, l.name, l.version, l.ghuser, l.ghrepo)).toSeq,
      "tags" -> l.tags,
      "site" -> l.size,
      "docs" -> l.docs,
      "resolvers" -> l.resolvers,
      "dependencies" -> Obj(
        "libraries" -> l.dependencies.libraries.map { dl =>
          Obj(
            "organization" -> dl.organization,
            "name" -> dl.name,
            "version" -> dl.version
          )
         }
      ),
      "ghuser" -> l.ghuser,
      "ghrepo" -> l.ghrepo
    )

  private def innerList(m: Obj)(objName: String, lname: String) =
    wrapDBList(wrapDBObj(m.getAs[BasicDBList](objName).get).getAs[BasicDBList](lname).get)

  private def moduleId(o: DBObject) =
   ModuleID(o.getAs[String]("organization").get,
            o.getAs[String]("name").get,
            o.getAs[String]("version").get)

  implicit val m2l: Obj => Library = (m) => Library(
    m.getAs[String]("organization").get,
    m.getAs[String]("name").get,
    m.getAs[String]("version").get,
    m.getAs[String]("description").get,
    wrapDBList(m.getAs[BasicDBList]("tags").get)
      .iterator.map(_.toString).toSeq,
    m.getAs[String]("site").get,
    m.getAs[String]("docs").get,
    wrapDBList(m.getAs[BasicDBList]("resolvers").get)
      .iterator.map(_.toString).toSeq,
    Dependencies(
      innerList(m)("dependencies", "libraries").iterator.map(l =>
        moduleId(wrapDBObj(l.asInstanceOf[DBObject]))).toSeq
    ),
    m.getAs[String]("ghuser"),
    m.getAs[String]("ghrepo")
  )

  implicit val mc2l: MongoCollection => Iterable[Library] =
    (m) => for(l <- m) yield m2l(l)

  implicit val misl: Iterator[DBObject] => Iterable[Library] =
    (m) => (for(l <- m) yield m2l(l)).toSeq

  implicit def mdbo2optpp(dbo: DBObject) = new {
    def opt(opt: Option[DBObject]): DBObject = opt match {
      case Some(other) => dbo ++ other
      case _ => dbo
    }
  }

 def any[T, C](q: String)(page: Int = 1, lim: Int = DefaultLimit)(f: Iterable[C] => T)
              (implicit cct: CanConvertListTo[C]) =
   Store.collection("libraries") { c =>
     f(cct( c.find("_keywords" $in (q.replaceAll("""\s+"""," ").split(" "))).skip(lim * (page - 1)).limit(lim) ))
   }

  def all[T, C](page: Int = 1, lim: Int = DefaultLimit)(f: Iterable[C] => T)
               (implicit cct: CanConvertListTo[C])=
    Store.collection("libraries") { c =>
      f(cct( c.find().skip(lim * (page - 1)).limit(lim) ))
    }

  def apply[T, C](user: String,
                  repo: Option[String] = None, version: Option[String] = None)
                (f: Iterable[C] => T)(implicit cct: CanConvertListTo[C]) =
    Store.collection("libraries") { c =>
      f(cct(
        c.find(Obj("ghuser" -> user) opt repo.map(r => Obj("ghrepo" -> r)))
      ))
    }

  // merge/update before simply appending to collection
  def save(libs: Seq[Library]) = libraries { col =>
        libs.map(l =>
          try { col.findAndModify(
           /*query*/Obj(
             "name" -> l.name, "organization" -> l.organization,"version" -> l.version,
             "ghuser" -> l.ghuser, "ghrepo" -> l.ghrepo
           ),
           /*fields*/Obj(),
           /*sort*/Obj(),
           /*rm*/false,
           /*update*/l2m(l),
           /*returnnew*/ true,
           /*upsert*/true
        ) } catch {
           case e => e.printStackTrace
        }
     )
  }
}
