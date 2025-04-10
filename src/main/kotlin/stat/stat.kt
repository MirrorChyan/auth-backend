package stat

import config.RDS
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport

private val log = LoggerFactory.getLogger("StatHelper")!!

object StatHelper {
    private val Q = LinkedBlockingQueue<String>()

    fun offer(s: String) {
        Q.offer(s)
    }

    fun poll() {
        Thread.startVirtualThread {
            log.info("start stat stat polling")
            while (true) {
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(60))
                val arr = arrayOfNulls<String>(Q.size)
                if (arr.isEmpty()) {
                    continue
                }
                for (i in arr.indices) {
                    arr[i] = Q.poll()
                }
                val date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now())
                val key = "key-stat:${date}"
                RDS.get().pfadd(key, *arr)
            }
        }
    }

}