package ls

trait Serializing {
  def serialize[T <: AnyRef](o: T): String
}

trait JsonSerializing extends Serializing {
  import org.json4s._
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization.{read, write}
  implicit val formats = Serialization.formats(NoTypeHints)

  def serialize[T <: AnyRef](o: T): String = write(o)
}
