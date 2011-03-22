package implicitly.stores

/** Interface for a KeyClass -> V store */
trait Store[V, E] {
  val domainCls: Class[V]

  type KeyClass

  def save[T](v: V)(fn: Either[E, V] => T): T

  def get(k: KeyClass): Either[E, V]

  def update[T](k: KeyClass, fn: Either[E, V] => T): T

  def delete(k: KeyClass)
}
