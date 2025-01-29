package cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.phantomthief.collection.BufferTrigger
import datasource.DB
import model.LogRecord
import model.entity.OperationLog
import org.ktorm.dsl.QueryRowSet
import org.ktorm.dsl.batchInsert
import java.util.concurrent.TimeUnit


val C: Cache<String, QueryRowSet> = Caffeine.newBuilder()
    .maximumSize(100)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .softValues()
    .build()

var BT: BufferTrigger<LogRecord> = run {
    BufferTrigger.batchBlocking<LogRecord>()
        .bufferSize(1000)
        .batchSize(500)
        .linger(30, TimeUnit.SECONDS)
        .setConsumerEx {
            Thread.startVirtualThread {
                val l = ArrayList(it)
                DB.batchInsert(OperationLog) {
                    l.forEach { v ->
                        item {
                            set(OperationLog.cdk, v.cdk)
                            set(OperationLog.source, v.source)
                            set(OperationLog.ua, v.ua)
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


