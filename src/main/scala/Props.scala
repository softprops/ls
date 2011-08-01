package implicitly


abstract class Props(resource: String) {
  protected lazy val underlying = {
    val props = new java.util.Properties()
    props.load(getClass().getResourceAsStream(resource))
    props
  }
}

object Props extends Props("/mongo.properties") {
  import scala.util.control.Exception.allCatch

  def get(name: String) = underlying.getProperty(name) match {
    case null => error("undefined property %s" format name)
    case value => value
  }

  def getInt(name: String) = allCatch.opt { get(name) toInt } match {
    case None => error("undefined int property %s" format name)
    case Some(n) => n
  }

  def apply(name: String) = underlying.getProperty(name) match {
     case null => None
     case value => Some(value)
  }

  def int(name: String) = apply(name).map(v => allCatch.opt{ v.toInt })
}
