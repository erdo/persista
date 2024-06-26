package co.early.persista

import co.early.fore.kt.core.coroutine.awaitCustom
import co.early.fore.kt.core.coroutine.launchCustom
import co.early.fore.kt.core.logging.Logger
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.Executors
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Copyright © 2021-24 early.co. All rights reserved.
 */
class PerSista(
    private val dataDirectory: File,
    @PublishedApi internal val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    @PublishedApi internal val writeReadDispatcher: CoroutineDispatcher = Executors.newSingleThreadExecutor()
        .asCoroutineDispatcher(),
    @PublishedApi internal val logger: Logger? = null,
    @PublishedApi internal val strictMode: Boolean = false,
    @PublishedApi internal val json: Json = Json,
) {

    init {
        getPersistaFolder().mkdirs()
    }

    inline fun <reified T : Any> write(item: T, crossinline complete: (T) -> Unit) {
        launchCustom(mainDispatcher) {
            complete(write(item, typeOf<T>()))
        }
    }

    fun <T : Any> write(item: T, type: KType, complete: (T) -> Unit) {
        launchCustom(mainDispatcher) {
            complete(write(item, type))
        }
    }

    inline fun <reified T : Any> read(default: T, crossinline complete: (T) -> Unit) {
        launchCustom(mainDispatcher) {
            complete(read(default, typeOf<T>()))
        }
    }

    fun <T : Any> read(default: T, type: KType, complete: (T) -> Unit) {
        launchCustom(mainDispatcher) {
            complete(read(default, type))
        }
    }

    inline fun <reified T : Any> clear(klass: KClass<out T>, crossinline complete: () -> Unit) {
        launchCustom(mainDispatcher) {
            clear(klass)
            complete()
        }
    }

    fun wipeEverything(complete: () -> Unit) {
        launchCustom(mainDispatcher) {
            wipeEverything()
            complete()
        }
    }

    suspend inline fun <reified T : Any> write(item: T): T {
        return write(item, typeOf<T>())
    }

    suspend fun <T : Any> write(item: T, type: KType): T {
        @Suppress("UNCHECKED_CAST")
        val klass = type.classifier as KClass<T>
        val qualifiedName = getQualifiedName(klass, strictMode, logger) ?: return item
        return awaitCustom(writeReadDispatcher) {
            qualifiedName.let { className ->
                try {
                    val serializer = serializer(type)
                    val jsonText = json.encodeToString(serializer, item)
                    logger?.d("WRITING to $className")
                    logger?.d(jsonText)
                    getKeyFile(klass)?.writeText(jsonText, Charsets.UTF_8)
                    item
                } catch (e: Exception) {
                    logger?.e(
                        "write failed (did you remember to add the kotlin serialization " +
                                "plugin to gradle? did you remember to add proguard rules for " +
                                "obfuscation? if you are using generics did you use the functions " +
                                "that let you specify the KType? typeOf<MyClass>. See the sample " +
                                "app and unit tests in the PerSista repo)",
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
        return read(default, typeOf<T>())
    }

    suspend fun <T : Any> read(default: T, type: KType): T {
        @Suppress("UNCHECKED_CAST")
        val klass = type.classifier as KClass<T>
        val qualifiedName = getQualifiedName(klass, strictMode, logger) ?: return default
        return awaitCustom(writeReadDispatcher) {
            qualifiedName.let { className ->
                try {
                    val serializer = serializer(type)
                    logger?.d("READING from $className")
                    getKeyFile(klass)?.readText(Charsets.UTF_8)?.let { jsonText ->
                        logger?.d(jsonText)
                        @Suppress("UNCHECKED_CAST")
                        json.decodeFromString(serializer, jsonText) as T
                    } ?: default
                } catch (e: Exception) {
                    when (e) {
                        is FileNotFoundException -> {
                            logger?.e("file not found, maybe it was never written? or " +
                                    "if you are manually specifying the type, maybe you have " +
                                    "specified the wrong type (usage: read(\"myString\", typeOf<String>())", e)
                        }
                        else -> {
                            logger?.e(
                                "read failed (did you remember to add the kotlin serialization " +
                                        "plugin to gradle? did you remember to add PROGUARD rules for " +
                                        "obfuscation? if you are using generics did you use the functions " +
                                        "that let you specify the KType? typeOf<MyClass>. See the sample " +
                                        "app and unit tests in the PerSista repo)",
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

    suspend inline fun <reified T : Any> clear(klass: KClass<out T>) {
        val qualifiedName = getQualifiedName(klass, strictMode, logger) ?: return
        awaitCustom(writeReadDispatcher) {
            qualifiedName.let { className ->
                try {
                    logger?.d("CLEARING $className")
                    getKeyFile(klass)?.delete()
                } catch (e: Exception) {
                    logger?.e("clear failed", e)
                    if (strictMode) {
                        throw e
                    } else {
                        return@let Unit
                    }
                }
            }
        }
    }

    suspend fun wipeEverything() {
        logger?.d("wipeEverything()")
        awaitCustom(writeReadDispatcher) {
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
