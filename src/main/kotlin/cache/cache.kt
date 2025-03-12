package cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.phantomthief.collection.BufferTrigger
import datasource.DB
import model.LogRecord
import model.ValidTuple
import model.entity.OperationLog
import org.ktorm.dsl.batchInsert
import java.util.concurrent.TimeUnit


val C: Cache<String, ValidTuple> = Caffeine.newBuilder()
    .expireAfterWrite(12, TimeUnit.HOURS)
    .softValues()
    .build()

val CT_CACHE: Cache<String, Set<String>> = Caffeine.newBuilder()
    .expireAfterWrite(24, TimeUnit.HOURS)
    .build()

var BT: BufferTrigger<LogRecord> = run {
    BufferTrigger.batchBlocking<LogRecord>()
        .bufferSize(1000)
        .batchSize(500)
        .linger(5, TimeUnit.SECONDS)
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


