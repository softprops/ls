package implicitly.stores

import implicitly.models.Project
import com.google.appengine.api.datastore.Blob
import javax.jdo.annotations._

object ProjectStore extends jdo.JdoStore[Project] with jdo.DefaultManager {
  override val domainCls = classOf[Project]
  type KeyClass = String /* url */

}
