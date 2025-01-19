package metrics

import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry


var registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)


fun initMetrics() {
    JvmThreadMetrics().bindTo(registry)
    JvmGcMetrics().bindTo(registry)
    JvmMemoryMetrics().bindTo(registry)
    UptimeMetrics().bindTo(registry)
}