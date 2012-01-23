organization := "me.lessis"

name := "linted-fails"

version := "0.1.0"

seq(lsSettings: _*)

TaskKey[Unit]("ls-lint-verify", "verifies ls-lint task, expects failure") <<= (LsKeys.lint) map {
  (lint) => if(lint) sys.error(
    "lint should have failed for invalid json"
  )
}
