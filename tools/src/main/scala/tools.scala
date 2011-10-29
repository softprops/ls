package ls

import sbt._
import sbt.Keys._
import sbt.Process._

object Tools extends sbt.Plugin {
  lazy val mongoDump = TaskKey[Unit]("mongo-dump", "...")
  lazy val mongoRestore = TaskKey[Unit]("mongo-restore", "...")
  
  override def settings: Seq[Setting[_]] = Seq (
    mongoDump <<= mongoDumpTask,
    mongoRestore <<= mongoRestoreTask
  )

  private def mongoDumpTask =
    (streams) map {
      (out) =>
        val uri = new java.net.URI(Props.get("MONGOLAB_URI"))
        val Array(user, pass) = uri.getUserInfo.split(":")
        val toDir = "."
        //mongodump -h dbh00.mongolab.com:27007 -d heroku_app1234 -u heroku_app1234 -p random_password -o dump-dir
        Process("mongodump -h %s:%s -d %s -u %s -p %s -o %" format(
          uri.getHost, uri.getPort, uri.getPath.drop(1), user, pass, toDir
        )) !!(out.log)
        ()
    }

  private def mongoRestoreTask =
    (streams) map {
      (out) =>
        val uri = new java.net.URI(Props.get("MONGOLAB_URI"))
        val Array(user, pass) = uri.getUserInfo.split(":")
        val fromDir = "."
        // mongorestore -h <new host>:<new port> -d heroku_app1234 -u heroku_app1234 -p <new_password> dump-dir/*
        Process("mongorestore -h %s:%s -d %s -u %s -p %s -o %" format(
          uri.getHost, uri.getPort, uri.getPath.drop(1), user, pass, fromDir
        )) !!(out.log)
        ()
    }
}
