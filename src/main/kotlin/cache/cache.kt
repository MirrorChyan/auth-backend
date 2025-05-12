package cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.phantomthief.collection.BufferTrigger
import config.RDS
import datasource.DB
import model.DownloadRecord
import model.LogRecord
import model.ValidTuple
import model.entity.DownloadLog
import model.entity.OperationLog
import org.ktorm.dsl.batchInsert
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger("CacheKt")!!

val C: Cache<String, ValidTuple> = Caffeine.newBuilder()
    .expireAfterWrite(7, TimeUnit.DAYS)
    .build()

val CT_CACHE: Cache<String, Set<String>> = Caffeine.newBuilder()
    .expireAfterWrite(7, TimeUnit.DAYS)
    .build()

val BT: BufferTrigger<LogRecord> = run {
    BufferTrigger.batchBlocking<LogRecord>()
        .bufferSize(1000)
        .batchSize(500)
        .linger(10, TimeUnit.SECONDS)
        .setConsumerEx {
            Thread.startVirtualThread {
                val l = ArrayList(it)
                DB.batchInsert(OperationLog) {
                    l.forEach { v ->
                        item {
                            set(OperationLog.cdk, v.cdk)
                            set(OperationLog.resource, v.resource)
                            set(OperationLog.ua, v.ua)
                            set(OperationLog.ip, v.ip)
                            set(OperationLog.type, v.type)
                            set(OperationLog.createdAt, v.time)
                        }
                    }
                }
            }
        }
        .build().apply {
            Runtime.getRuntime().addShutdownHook(Thread { manuallyDoTrigger() })
        }
}


val DOWNLOAD_TRIGGER: BufferTrigger<DownloadRecord> = run {
    BufferTrigger.batchBlocking<DownloadRecord>()
        .bufferSize(1000)
        .batchSize(500)
        .linger(10, TimeUnit.SECONDS)
        .setConsumerEx {
            Thread.startVirtualThread {
                val l = ArrayList(it)
                DB.batchInsert(DownloadLog) {
                    l.forEach { v ->
                        item {
                            set(DownloadLog.cdk, v.cdk)
                            set(DownloadLog.resource, v.resource)
                            set(DownloadLog.ua, v.ua)
                            set(DownloadLog.ip, v.ip)
                            set(DownloadLog.filesize, v.filesize)
                            set(DownloadLog.version, v.version)
                            set(DownloadLog.createdAt, v.time)
                        }
                    }
                }
            }
        }
        .build().apply {
            Runtime.getRuntime().addShutdownHook(Thread { manuallyDoTrigger() })
        }
}


fun evictAll() {
    log.info("evict cdk type cache size {}", CT_CACHE.estimatedSize())
    CT_CACHE.invalidateAll()
    log.info("evict cdk cache size {}", C.estimatedSize())
    C.invalidateAll()
}

fun doSubscribeEvictEvent() {
    RDS.subscribe()
}


