package ls

object Sbt {
  def lib(l: Library, cross: Boolean) =
    """ "%s" %s "%s" % "%s" """ format(l.organization, if(cross) "%%" else "%s", l.name, l.version)

  def configuration(l: Library) = {
    """libraryDependencies += %s""" format(lib(l, true))
  }
}
