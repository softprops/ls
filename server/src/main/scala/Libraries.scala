package ls

import com.mongodb.casbah.Imports._

object Conversions extends Logged {
  import com.mongodb.casbah.commons.Imports.ObjectId
  import com.mongodb.casbah.commons.{MongoDBObject => Obj, MongoDBList => ObjList}
  import com.mongodb.casbah.{MongoCollection, MongoCursor}
  import com.mongodb.{BasicDBList, DBObject}
  import com.mongodb.casbah.Implicits._
  import java.util.Date

  // todo complete this
  val StopWords = Seq(
    "a", "and", "as", "at", "but", "for", "the"
  )

  private def keywords(l: Library) =
    l.description.replaceAll("""\s+"""," ").toLowerCase.split(" ").map(_.trim) ++
      l.tags.map(_.trim) ++ Seq(
        l.organization, l.name, l.version,
        l.ghuser, l.ghrepo
      ).filterNot(
        StopWords.contains(_)
      )

  private def keywords(l: LibraryVersions) = 
    l.description.replaceAll("""\s+"""," ").toLowerCase.split(" ").map(_.trim) ++
      l.tags.map(_.trim) ++ Seq(
        l.organization, l.name, /*l.version, */
        l.ghuser, l.ghrepo
      ).filterNot(
        StopWords.contains(_)
      )

  implicit val libraryVersionsToDbObject: LibraryVersions => DBObject =
    (l: LibraryVersions) =>
      Obj(
      "organization" -> l.organization,
      "name" -> l.name,
      "description" -> l.description,
      "_keywords" -> keywords(l),
      "tags" -> l.tags,
      "site" -> l.site,
      "sbt" -> l.sbt,
      "ghuser" -> l.ghuser,
      "ghrepo" -> l.ghrepo,
      "updated" -> new Date().getTime,
      "contributors" ->
        l.contributors.getOrElse(Nil).map { c =>
          Obj(
            "login" -> c.login,
            "id" -> c.id,
            "avatar_url" -> c.avatar_url,
            "url" -> c.url
          )
        },
      "versions" -> versionsToDbObjects(l.versions)
    )

  def libraryDepToDbObject(m: ModuleID) =
    Obj(
      "organization" -> m.organization,
      "name" -> m.name,
      "version" -> m.version
    )

  def licenseToDbObject(l: License) = l match {
    case License(n, u) => Obj(
      "name" -> n,
      "url" -> u
    )
  }

  implicit val libraryToDbObject: Library => DBObject =
    (l: Library) =>
      Obj(
        "organization" -> l.organization,
        "name" -> l.name,
        "description" -> l.description,
        "_keywords" -> keywords(l),
        "tags" -> l.tags,
        "site" -> l.site,
        "sbt" -> l.sbt,
        "ghuser" -> l.ghuser,
        "ghrepo" -> l.ghrepo,
        "contributors" ->
          l.contributors.getOrElse(Nil).map { c =>
            Obj(
              "login" -> c.login,
              "id" -> c.id,
              "avatar_url" -> c.avatar_url,
              "url" -> c.url
            )
          },
        "updated" -> new Date().getTime,
        "versions" ->
          Seq(Obj( // this should NOT delete any exisiting versions
            "version" -> l.version,
            "docs" -> l.docs,
            "resolvers" -> l.resolvers,
            "dependencies" ->
              l.dependencies.map(libraryDepToDbObject),
            "licenses" -> l.licenses.map(licenseToDbObject),
            "scalas" -> l.scalas
          ))
      )

  private def versionsToDbObjects(versions: Seq[Version]) =
    versions.map { v =>
      Obj(
        "version" -> v.version,
        "docs" -> v.docs,
        "resolvers" -> v.resolvers,
        "dependencies" ->
          v.dependencies.map(libraryDepToDbObject),
          "licenses" -> v.licenses.map(licenseToDbObject),
          "scalas" -> v.scalas
      )
    }

  implicit val versionOrdering: Ordering[Version] = Ordering.by((_:Version).version).reverse

  //private def innerList(m: Obj)(objName: String, lname: String) =
  //  wrapDBList(wrapDBObj(m.getAs[BasicDBList](objName).get).getAs[BasicDBList](lname).get)

  def first(m: Obj)(objName: String, prop: String): Option[String] =
    wrapDBList(m.getAs[BasicDBList](objName).get).iterator match {
      case it if(it hasNext) =>
        it.next.asInstanceOf[DBObject].getAs[String](prop)
      case _ => None
    }

  private def moduleId(o: DBObject) =
    ModuleID(o.getAs[String]("organization").get,
            o.getAs[String]("name").get,
            o.getAs[String]("version").get)

  private def contributor(o: DBObject) =
    User(o.getAs[String]("login").get,
         o.getAs[Int]("id").get,
         o.getAs[String]("avatar_url").get,
         o.getAs[String]("url").get)

  private def license(o: DBObject) =
    License(o.getAs[String]("name").get,
     o.getAs[String]("url").get)

  private def dbObjectToVersion(m:Obj) = try { Version(
    m.getAs[String]("version").get,
    m.getAs[String]("docs").get,
    wrapDBList(m.getAs[BasicDBList]("resolvers").get)
      .iterator.map(_.toString).toSeq,
    wrapDBList(m.getAs[BasicDBList]("dependencies").get)
      .iterator.map(l => moduleId(wrapDBObj(l.asInstanceOf[DBObject]))).toSeq,
    wrapDBList(m.getAs[BasicDBList]("scalas").get)
      .iterator.map(_.toString).toSeq,
    m.getAs[BasicDBList]("licenses") match {
      case Some(dbl) =>
        wrapDBList(dbl).iterator.map(l => license(l.asInstanceOf[DBObject])).toSeq
      case _ => Seq()
    }
  ) } catch {
    case e =>
      log.error("failed to parse %s" format m)
      throw e
  }

  implicit val dbObjectToLibraryVersions: Obj => LibraryVersions = (m) => try { LibraryVersions(
    m.getAs[String]("organization").get,
    m.getAs[String]("name").get,
    m.getAs[String]("description").get,
    m.getAs[String]("site").get,
    wrapDBList(m.getAs[BasicDBList]("tags").get)
      .iterator.map(_.toString).toSeq,
    m.getAs[Long]("updated").getOrElse(new Date().getTime),
    wrapDBList(m.getAs[BasicDBList]("versions").get)
      .iterator.map(v => dbObjectToVersion(v.asInstanceOf[BasicDBObject])).toSeq,
    m.getAs[Boolean]("sbt").getOrElse(false),
    m.getAs[String]("ghuser"),
    m.getAs[String]("ghrepo"),
    m.getAs[BasicDBList]("contributors") match {
      case Some(dbl) =>
        Some(wrapDBList(dbl)
             .iterator.map(c => 
               contributor(wrapDBObj(c.asInstanceOf[DBObject]))
              ).toSeq)
      case _ => None
    }
  ) } catch {
    case e =>
      log.error("failed to parse %s" format m)
      throw e
  }

  implicit val mc2lv: MongoCollection => Iterable[LibraryVersions] = 
    (m) => for(l <- m) yield dbObjectToLibraryVersions(l)

  implicit val mislv: Iterator[DBObject] => Iterable[LibraryVersions] =
    (m) => (for(l <- m) yield dbObjectToLibraryVersions(l)).toSeq

  // todo: contribute back to casbah
  implicit def mdbo2optpp(dbo: DBObject) = new {
    def opt(opt: Option[DBObject]): DBObject = opt match {
      case Some(other) => dbo ++ other
      case _ => dbo
    }
  }
}

object Libraries extends Logged {
  import Conversions._
  import com.mongodb.casbah.commons.Imports.ObjectId
  import com.mongodb.casbah.commons.{MongoDBObject => Obj, MongoDBList => ObjList}
  import com.mongodb.casbah.{MongoCollection}
  import com.mongodb.{BasicDBList, DBObject}
  import com.mongodb.casbah.Implicits._

  type CanConvertListTo[A] = Iterator[DBObject] => Iterable[A]
  type CanConvertTo[A, B] = A => B

  val DefaultLimit = 20

  private def libraries[T](f: MongoCollection => T) =
    Store.collection("libraries")(f)

  /** searches for librarues by author/contributors */
  def author[T, C](ghuser: String)(f: Iterable[C] => T)
                  (implicit cct: CanConvertListTo[C]) =
    libraries { c =>
      log.info("getting libraries for author %s" format ghuser)
      f(cct(c.find(
        $or(
          "ghuser" -> ghuser,
           "contributors.login" -> ghuser
        )))
      )
    }

  /** search by any search terms */
  def any[T, C](terms: Seq[String])
              (page: Int = 1, lim: Int = DefaultLimit)(f: Iterable[C] => T)
              (implicit cct: CanConvertListTo[C]) =
   libraries { c =>
     log.info("getting libraries for terms %s" format terms.mkString(", "))
     val possiblies =  (MongoDBObject().empty /: terms)(
       (a, e) => a += ("name" -> """(?i)%s""".format(e).r)
     )
     val parts = (possiblies ++ ("_keywords" $in terms)).toMap
     val query = $or(parts.toSeq:_*)
     log.info("any query: %s" format query)
     f(cct( paginate(c.find(query), page, lim).sort(Obj("updated" -> -1)) ))
   }

  // this will always return one less and one more
  // for pagination hints
  def paginate(c: MongoCursor, page: Int, lim: Int) =
    c.skip(math.max(0, (lim * (page - 1)) - 1)).limit(lim + 1)

  /** get a pagingates list of all libraries */
  def all[T, C](page: Int = 1, lim: Int = DefaultLimit)(f: Iterable[C] => T)
               (implicit cct: CanConvertListTo[C])=
    Store.collection("libraries") { c =>
      log.info("getting libraries (page: %s, lim: %s)" format(page, lim))
      f(cct( paginate(c.find(), page, lim).sort(Obj("updated" -> -1)) ))
    }

  /** For project-based queries */
  def projects[T, C](user: String,
                     repo: Option[String] = None)
                    (f: Iterable[C] => T)(implicit cct: CanConvertListTo[C]) =
    libraries { c =>
      log.info("getting libraries for user: %s, repo: %s" format(
        user, repo
      ))
      val query =
        Obj("ghuser" -> user) opt repo.map(r => Obj("ghrepo" -> r))
      log.info("query: %s" format query)
      f(cct(
        c.find(query)
      ))
    }

  def latest[T, C](name: String, user: Option[String] = None,
                   repo: Option[String] = None)
                    (f: Iterable[C] => T)(implicit cct: CanConvertListTo[C]) =
    libraries { c =>
      log.info("geting latest version of %s (%s/%s)" format(name, user, repo))
      val query = Obj("name" -> name) opt user.map(u =>
        Obj("ghuser" -> u)
      ) opt repo.map(r =>
        Obj("ghrepo" -> r)
      )
      f(cct(c.find(
        query,
        Obj("_id" -> 0, "versions.version" -> 1)
      )))
    }

  /** Find by name + version and optionally user and repo */
  def apply[T, C](name: String,
                  version: Option[String] = None,
                  user: Option[String] = None,
                  repo: Option[String] = None)
                (f: Iterable[C] => T)(implicit cct: CanConvertListTo[C]) =
    libraries { c =>
      log.info("getting libraries for name: %s, user: %s, repo: %s" format(
        name, user, repo
      ))
      val query =
        Obj("name" -> name) opt user.map(u =>
          Obj("ghuser" -> u)
        ) opt repo.map(r =>
          Obj("ghrepo" -> r)
        ) opt version.map(v =>
          Obj("versions.version" -> version)
        )
      log.info("query: %s" format query)
      f(cct(
        c.find(query)
      ))
    }

  // merge/update before simply appending to collection
  // potential hammersmith candiate
  def save(libs: Seq[Library]) = libraries { col =>
    log.info("saving or updating %d libraries" format libs.size)
    libs.map { l =>

      val query = Obj(
        "name" -> l.name, "organization" -> l.organization,
        "ghuser" -> l.ghuser, "ghrepo" -> l.ghrepo
      )
      log.info("create or update selection query %s" format query)

      apply(l.name, user = l.ghuser, repo = l.ghrepo){ (currentVersions: Iterable[LibraryVersions]) =>
        if(currentVersions.isEmpty) try { col.findAndModify(
          query, // query
          Obj(), // fields
          Obj(), // sort
          false, // rm 
          libraryToDbObject(l),// update
          true,  // returned new
          true   // upset
        ) } catch {
          case e => e.printStackTrace
        } else {
          // this could get ugly!
          val current: LibraryVersions = currentVersions.head.copy(
            description = l.description,
            site = l.site,
            tags = l.tags,
            sbt = l.sbt,
            contributors = l.contributors
          )
          val versions = current.versions.toSeq
          val (contained, notcontained) = versions.partition(_.version == l.version)
          if(contained.isEmpty) {
            val appended = (Version(
              l.version, l.docs,
              l.resolvers, l.dependencies,
              l.scalas,
              l.licenses
            ) +: versions).sorted
            val updating = libraryVersionsToDbObject(current.copy(
              versions = appended
            ))
            // append version
            col.findAndModify(
              query,
              Obj(),
              Obj(),
              false, 
              updating,
              true,
              true
            )
          } else {
            // update version
            val merged = (contained(0).copy(
              version = l.version,docs = l.docs,
              resolvers = l.resolvers, dependencies = l.dependencies,
              scalas = l.scalas
            ) +: notcontained).sorted
            val updated = libraryVersionsToDbObject(current.copy(
              versions = merged
            ))
            col.findAndModify(
              query,
              Obj(),
              Obj(),
              false, 
              updated,
              true,
              true
            )            
          }
        }
      }
    }
  }
}
