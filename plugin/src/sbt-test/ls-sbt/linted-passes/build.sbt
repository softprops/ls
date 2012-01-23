organization := "me.lessis"

name := "linted-passes"

version := "0.1.0"

seq(lsSettings: _*)

TaskKey[Unit]("ls-lint-verify", "verifies ls-lint task, expects pass") <<= (LsKeys.lint) map {
  (lint) => if(!lint) sys.error(
    "lint should have passed for valid json"
  )
}
