package co.early.persista

import co.early.fore.core.coroutine.awaitCustom
import co.early.fore.core.coroutine.awaitIO
import co.early.fore.core.coroutine.launchCustom
import co.early.fore.core.coroutine.launchIO
import co.early.fore.core.coroutine.launchMain
import co.early.fore.core.delegate.Fore
import co.early.fore.core.logging.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.FileNotFoundException
import okio.Path
import okio.SYSTEM
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Copyright © 2021-25 early.co. All rights reserved.
 */
class PerSista(
    private val dataPath: Path,
    @PublishedApi internal val encryptor: Encryptor? = null,
    @PublishedApi internal val logger: Logger? = null,
    @PublishedApi internal val strictMode: Boolean = false,
    @PublishedApi internal val json: Json = Json,
) {

    // this is for iOS target benefit which doesn't like default parameters in constructors
    constructor(dataPath: Path) : this(dataPath, null, null, false, Json)
    constructor(dataPath: Path, logger: Logger) : this(dataPath, null, logger, false, Json)
    constructor(dataPath: Path, logger: Logger, json: Json) : this(dataPath, null, logger, false, json)
    constructor(dataPath: Path, encryptor: Encryptor) : this(dataPath, encryptor, null, false, Json)
    constructor(dataPath: Path, encryptor: Encryptor, logger: Logger) : this(dataPath, encryptor, logger, false, Json)
    constructor(dataPath: Path, encryptor: Encryptor, logger: Logger, json: Json) : this(dataPath, encryptor, logger, false, json)

    private val writeReadMutex = Mutex()

    init {
        launchIO {
            writeReadMutex.withLock {
                okio.FileSystem.SYSTEM.createDirectories(getPersistaFolder())
            }
        }
    }

    inline fun <reified T : Any> write(item: T, crossinline complete: (T) -> Unit) {
        launchMain {
            complete(write(item, typeOf<T>()))
        }
    }

    fun <T : Any> write(item: T, type: KType, complete: (T) -> Unit) {
        launchMain {
            complete(write(item, type))
        }
    }

    inline fun <reified T : Any> read(default: T, crossinline complete: (T) -> Unit) {
        launchMain {
            complete(read(default, typeOf<T>()))
        }
    }

    fun <T : Any> read(default: T, type: KType, complete: (T) -> Unit) {
        launchMain {
            complete(read(default, type))
        }
    }

    inline fun <reified T : Any> clear(klass: KClass<out T>, crossinline complete: () -> Unit) {
        launchMain {
            clear(klass)
            complete()
        }
    }

    fun wipeEverything(complete: () -> Unit) {
        launchMain {
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
        return awaitIO {
            try {
                val serializer = serializer(type)
                val jsonText = json.encodeToString(serializer, item)
                logger?.d("PerSista WRITING to $qualifiedName")
                logger?.v(jsonText)
                getKeyFile(klass)?.let { path ->
                    writeReadMutex.withLock {
                        okio.FileSystem.SYSTEM.write(path) {
                            writeUtf8(encryptor?.encrypt(jsonText) ?: jsonText)
                        }
                    }
                }
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

    suspend inline fun <reified T : Any> read(default: T): T {
        return read(default, typeOf<T>())
    }

    suspend fun <T : Any> read(default: T, type: KType): T {
        @Suppress("UNCHECKED_CAST")
        val klass = type.classifier as KClass<T>
        val qualifiedName = getQualifiedName(klass, strictMode, logger) ?: return default
        return awaitIO {
            try {
                val serializer = serializer(type)
                logger?.d("PerSista READING from $qualifiedName")
                getKeyFile(klass)?.let { path ->
                    writeReadMutex.withLock {
                        okio.FileSystem.SYSTEM.read(path) {
                            val cipherText = readUtf8()
                            val jsonText = encryptor?.decrypt(cipherText) ?: cipherText
                            logger?.v(jsonText)
                            @Suppress("UNCHECKED_CAST")
                            json.decodeFromString(serializer, jsonText) as T
                        }
                    }
                } ?: default
            } catch (e: Exception) {
                when (e) {
                    is FileNotFoundException -> {
                        logger?.w(
                            "file not found, maybe it was never written? (if that's " +
                                    "expected - for example it's the first time you run this app, " +
                                    "you can ignore this warning) or " +
                                    "if you are manually specifying the type, maybe you have " +
                                    "specified the wrong type (usage: read(\"myString\", typeOf<String>())",
                            e
                        )
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

    suspend fun <T : Any> clear(klass: KClass<out T>) {
        val qualifiedName = getQualifiedName(klass, strictMode, logger) ?: return
        awaitIO {
            try {
                logger?.d("PerSista CLEARING $qualifiedName")
                getKeyFile(klass)?.let { path ->
                    writeReadMutex.withLock {
                        okio.FileSystem.SYSTEM.delete(path)
                    }
                }
            } catch (e: Exception) {
                logger?.e("clear failed", e)
                if (strictMode) {
                    throw e
                } else {
                    Unit
                }
            }
        }
    }

    suspend fun wipeEverything() {
        logger?.d("PerSista wipeEverything()")
        awaitIO {
            getPersistaFolder().let {
                writeReadMutex.withLock {
                    okio.FileSystem.SYSTEM.deleteRecursively(it)
                    okio.FileSystem.SYSTEM.createDirectories(it)
                }
            }
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
    internal fun <T : Any> getKeyFile(clazz: KClass<T>): Path? {
        return clazz.qualifiedName?.let { fileName ->
            getKeyFile(fileName)
        }
    }

    private fun getKeyFile(className: String): Path {
        return getPersistaFolder() / className
    }

    private fun getPersistaFolder(): Path {
        return dataPath / "persista"
    }
}

interface Encryptor {
    fun encrypt(plainText: String): String
    fun decrypt(base64CipherText: String): String?
}
