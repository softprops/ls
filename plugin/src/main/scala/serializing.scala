package ls

trait Serializing {
  def serialize[T <: AnyRef](o: T): String
}

trait LiftJsonSerializing extends Serializing {
  import net.liftweb.json._
  import net.liftweb.json.Serialization.{read, write}
  implicit val formats = Serialization.formats(NoTypeHints)

  def serialize[T <: AnyRef](o: T): String = write(o)
}
