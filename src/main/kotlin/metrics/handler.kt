package metrics

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry


var registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)


fun initMetrics() {
    registry.config().commonTags("application", "mirrorc-cdk-backend")
    JvmThreadMetrics().bindTo(registry)
    JvmGcMetrics().bindTo(registry)
    JvmMemoryMetrics().bindTo(registry)
    ProcessorMetrics().bindTo(registry)
    FileDescriptorMetrics().bindTo(registry)
    LogbackMetrics().bindTo(registry)
    JvmHeapPressureMetrics().bindTo(registry)
    ClassLoaderMetrics().bindTo(registry)
    UptimeMetrics().bindTo(registry)
}