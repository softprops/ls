package implicitly

import dispatch.HttpExecutor

trait SilentLogging {
  def httpLogger = new dispatch.Logger { def info(msg: String, args: Any*) {} }
}

trait GaeHttp

object GaeHttp extends SilentLogging {
   implicit def http = new dispatch.gae.Http {
     override def make_logger = httpLogger
   }
}

trait SimpleHttp

object SimpleHttp extends SilentLogging {
   implicit def http = new dispatch.Http {
     override def make_logger = httpLogger
   }
}
