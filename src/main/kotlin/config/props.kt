package config

import com.moandjiezana.toml.Toml
import java.io.File

private const val CFG_NAME = "config.toml"

private object TomlStore {
    init {
        printBanner()
    }

    private fun printBanner() {
        this::class.java.classLoader.getResourceAsStream("banner.txt")?.use {
            println(String(it.readAllBytes()))
        }

    }

    val config: Lazy<Toml> = lazy(mode = LazyThreadSafetyMode.NONE) {
        for (it in searchPath()) {
            val file = File(it)
            if (file.exists() || file.isFile) {
                return@lazy Toml().read(file)
            }
        }
        throw IllegalStateException("未找到配置文件 $CFG_NAME")
    }

    private fun searchPath(): List<String> {
        val current = System.getProperty("user.dir")
        return listOf(
            "$current/config/$CFG_NAME",
            "$current/$CFG_NAME",
        )
    }

}

object Props {
    object Datasource {
        val url: String by lazy { get("url") }
        val username: String by lazy { get("username") }
        val password: String by lazy { get("password") }
        val driver: String by lazy { get("driver") }


        fun get(key: String): String = TomlStore.config.value.getString("datasource.$key")
    }

    object Redis {
        val url: String by lazy {
            TomlStore.config.value.getString("redis.url")
        }
    }
}
