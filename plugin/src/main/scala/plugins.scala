package ls

object PluginSupport {
  val HelpTemplate = """
  |Dynamic evaluation and installation of plugin dependencies is not currently supported.
  |You can however append the following lines to your `plugins.sbt` configuration
  |
  |%s
  |
  |Consult the plugin's documentation for additional configuration options
  | %s"""

  def help(lib: LibraryVersions, version: Version, config: Seq[String]) =
    HelpTemplate.stripMargin.format(
      config.mkString("\n\n"),
      Seq(
        lib.site, version.docs,
        "https://github.com/%s/%s".format(lib.ghuser.get, lib.ghrepo.get)
      ).filterNot(_.isEmpty).map(" * %s" format _).mkString("\n")
    ).trim
}
