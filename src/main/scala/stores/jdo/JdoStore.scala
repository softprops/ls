package implicitly.stores.jdo

import javax.jdo.{JDOHelper, PersistenceManagerFactory,
                  PersistenceManager, Query}

import com.google.appengine.api.datastore.{Key, KeyFactory}

/** KeyClass key to V value */
trait JdoStore[V] extends implicitly.stores.Store[V, String]  { self: Managed =>

  val domainCls: Class[V]

  type KeyClass

  def save[T](v: V)(f: Either[String, V] => T): T = withManager { m =>
    f(try { Right(m.makePersistent(v)) } catch { case _ => Left("Error saving %s" format v) })
  }

  def get(k: KeyClass) = withManager { m =>
    try {
      m.getObjectById(domainCls, k) match {
        case null => Left("could not find object with key %s" format k)
        case v => Right(v.asInstanceOf[V])
      }
    } catch {
      case e: javax.jdo.JDOObjectNotFoundException => Left("could not find object with key %s" format k)
    }
  }

  def update[T](k: KeyClass, fn: Either[String, V] => T): T = withManager { m =>
    fn(get(k))
  }

  def delete(k: KeyClass) = withManager { m =>
    get(k) fold({ e => e }, { v =>
      m.deletePersistent(v)
    })
  }

  protected def query[T](q: String)(f: Query => T) = withManager { m =>
    f(m.newQuery(q))
  }
}
