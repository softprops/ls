package ls

import com.ning.http.client.{ Request, AsyncHandler }
import dispatch._, dispatch.Defaults._
import java.net.UnknownHostException

trait Requesting {
  def http[T](pair: (Request, AsyncHandler[T])): Future[T] = {
    val h = new Http
    try { Shared.http(pair) }
    catch {
      case uh: UnknownHostException => sys.error(
        "You may not know your host as well as you think. Your http client doesn't know %s" format uh.getMessage
      )
    }
  }
}
