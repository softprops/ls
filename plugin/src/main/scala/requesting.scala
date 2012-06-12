package ls

trait Requesting {
  import dispatch._
  import org.apache.http.conn.{ HttpHostConnectException => ConnectionRefused }

  def http[T](hand: Handler[T]): T = {
    val h = new Http with NoLogging
    try { h(hand) }
    catch {
      case cf: ConnectionRefused => sys.error(
        "ls is currently not available to take your call"
      )
      case uh:java.net.UnknownHostException => sys.error(
        "You may not know your host as well as you think. Your http client doesn't know %s" format uh.getMessage
      )
    }
    finally { h.shutdown() }
  }
}
