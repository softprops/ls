package implicitly.models

import javax.jdo.annotations._
import com.google.appengine.api.datastore.Blob

@PersistenceCapable( identityType = IdentityType.APPLICATION, detachable="true" )
class Project {

  /** url is defined by :user/:repo/:org/:module/:version  */
  @PrimaryKey
  @Persistent( valueStrategy = IdGeneratorStrategy.IDENTITY )
  var url: String = _

  @Persistent
  var resolvers: String = _

  @Persistent
  var scalaVersions: String = _

  @Persistent
  var createdAt: java.util.Date = new java.util.Date()

  @Persistent
  var parentProject: String = _
}
