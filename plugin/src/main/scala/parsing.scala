package ls

trait Parsing {
  def parseJson[T: Manifest](s: String): T
}

trait LiftJsonParsing extends Parsing {
//  import net.liftweb.json._
//  implicit val formats = DefaultFormats
  import org.json4s._
  import org.json4s.native.JsonMethods._
  implicit val formats = DefaultFormats
  def parseJson[T:  Manifest](s: String) = parse(s).extract[T]
}
