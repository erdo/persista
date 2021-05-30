package co.early.persista

import co.early.fore.kt.core.logging.Logger
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.Executors
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

/**
 * Copyright © 2021 early.co. All rights reserved.
 */
@ExperimentalStdlibApi
class PerSista(
    private val dataDirectory: File,
    @PublishedApi internal val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    @PublishedApi internal val writeReadDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor()
        .asCoroutineDispatcher(),
    @PublishedApi internal val logger: Logger? = null,
    @PublishedApi internal val strictMode: Boolean = false,
) {

    init {
        getPersistaFolder().mkdirs()
    }

    inline fun <reified T : Any> write(item: T, crossinline complete: (T) -> Unit) {
        CoroutineScope(mainDispatcher).launch {
            write(item)
            complete(item)
        }
    }

    inline fun <reified T : Any> read(default: T, crossinline complete: (T) -> Unit) {
        CoroutineScope(mainDispatcher).launch {
            complete(read(default))
        }
    }

    fun wipeEverything(complete: () -> Unit) {
        CoroutineScope(mainDispatcher).launch {
            wipeEverything()
            complete()
        }
    }

    suspend inline fun <reified T : Any> write(item: T): T {
        val klass = T::class
        val qualifiedName = getQualifiedName(klass, strictMode, logger)?: return item
        return withContext(writeReadDispatcher) {
            qualifiedName.let { className ->
                try {
                    val serializer = serializer(typeOf<T>())
                    val jsonText = Json.encodeToString(serializer, item)
                    logger?.d("WRITING to $className")
                    logger?.d("$jsonText")
                    getKeyFile(klass)?.writeText(jsonText, Charsets.UTF_8)
                    item
                } catch (e: Exception) {
                    logger?.e(
                        "write failed (did you remember to add the kotlin serialization " +
                                "plugin to gradle? did you remember to add proguard rules for " +
                                "obfuscation? see the sample app in the PerSista repo)",
                        e
                    )
                    if (strictMode) {
                        throw e
                    } else {
                        item
                    }
                }
            }
        }
    }

    suspend inline fun <reified T : Any> read(default: T): T {
        val klass = T::class
        val qualifiedName = getQualifiedName(klass, strictMode, logger) ?: return default
        return withContext(writeReadDispatcher) {
            qualifiedName.let { className ->
                try {
                    val serializer = serializer(typeOf<T>())
                    logger?.d("READING from $className")
                    getKeyFile(klass)?.readText(Charsets.UTF_8)?.let { jsonText ->
                        logger?.d("$jsonText")
                        Json.decodeFromString(serializer, jsonText) as T
                    } ?: default
                } catch (e: Exception) {
                    when (e) {
                        is FileNotFoundException -> {
                            logger?.e("file not found, maybe it was never written?", e);
                        }
                        else -> {
                            logger?.e(
                                "write failed (did you remember to add the kotlin serialization " +
                                        "plugin to gradle? did you remember to add proguard rules for " +
                                        "obfuscation? see the sample app in the PerSista repo)",
                                e
                            )
                            if (strictMode) {
                                throw e
                            }
                        }
                    }
                    default
                }
            }
        }
    }

    suspend fun wipeEverything() {
        logger?.d("wipeEverything()")
        withContext(writeReadDispatcher) {
            getPersistaFolder().deleteRecursively()
            getPersistaFolder().mkdirs()
        }
    }

    @PublishedApi
    internal fun <T : Any> getQualifiedName(
        klass: KClass<T>,
        strictMode: Boolean,
        logger: Logger?
    ): String? {
        val qName = klass.qualifiedName
        if (qName == null) {
            logger?.e("klass must have a qualified name (can't persist anonymous classes)")
            if (strictMode) {
                throw IllegalArgumentException("klass must have a qualified name (can't persist anonymous classes)")
            }
        }
        return qName
    }

    @PublishedApi
    internal fun <T : Any> getKeyFile(clazz: KClass<T>): File? {
        return clazz.qualifiedName?.let { fileName ->
            File(dataDirectory, getKeyFilePath(fileName))
        }
    }

    private fun getPersistaFolder(): File {
        return File(dataDirectory, getPersistaFolderPath())
    }

    private fun getKeyFilePath(className: String): String {
        return "${getPersistaFolderPath()}${File.separator}${className}"
    }

    private fun getPersistaFolderPath(): String {
        return "${File.separator}persista"
    }
}
