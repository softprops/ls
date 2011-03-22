package implicitly.stores.jdo

import javax.jdo.{JDOHelper, PersistenceManagerFactory, PersistenceManager}

import com.google.appengine.api.datastore.{Key, KeyFactory}

trait Managed {
  def manager: PersistenceManager
  def withManager[T](fn: PersistenceManager => T): T = {
    val pm = manager
    try {
      fn(pm)
    } finally {
      pm.close
    }
  }
}

object ManagerFactory {
  lazy val get = JDOHelper.getPersistenceManagerFactory("transactions-optional")
}

object EventualConsistencyManagerFactory {
  lazy val get = JDOHelper.getPersistenceManagerFactory("eventual-reads-short-deadlines")
}

trait DefaultManager extends Managed {
  private lazy val fact = ManagerFactory get
  def manager = fact getPersistenceManager
}

trait EventualConsistencyManager extends Managed {
  private lazy val fact = EventualConsistencyManagerFactory get
  def manager = fact getPersistenceManager
}
