package implicitly

object Ls {
  import net.liftweb.json.JsonAST._
  import net.liftweb.json.JsonDSL._
  import net.liftweb.json.JsonParser._

  import models.{Project, Projects}

  object Scala {
    def apply(src: String): List[(String, String) => Project] =
      for {
        JObject(json) <- parse(src)
        JField("id", JArray(List(JString(org), JString(module), JString(vers)))) <- json
        JField("description", JString(desc)) <- json
        JField("resolvers", JArray(resolvers)) <- json
        JField("scala-versions", JArray(versions)) <- json
        JField("parent", parent) <- json
      } yield {
        Projects((org, module, vers), desc,
         resolvers map { _ match { case JString(s) => s case _ => error("what?") } },
         versions map { _ match { case JString(s) => s case _ => error("what?") } },
         parent match {
           case JArray(list) => list.map { _ match { case JString(s) => s case _ => error("what?") } } match {
             case id :: name :: version :: Nil => Some(id, name, version)
             case _ => None
           }
           case JString(self) => None
           case _ => error("invalid format %s" format parent)
        })_
      }
  }
}
