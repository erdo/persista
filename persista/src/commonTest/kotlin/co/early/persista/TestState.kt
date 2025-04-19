package co.early.persista

import co.early.persista.TestState.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedClassSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class TestState {

    @Serializable
    data class DashboardState(
        val dashboardId: Int,
        val userName: String,
        val drivers: List<Driver> = emptyList(),
        val error: Error? = null,
        @Transient val isUpdating: Boolean = false
    )

    @Serializable
    data class Driver(
        val driverId: Int,
        val driverName: String,
        val powerLevel: Int,
        val lat: Int,
        val long: Int
    )

    @Serializable
    enum class Error {
        UNKNOWN,
        SECURITY,
        IO
    }

    @Serializable
    data class MoreState(
        val data: Boolean = false
    )

    data class SomethingElseState(
        val someId: Int,
        val someUserName: String,
    )

    @Serializable
    data class StateContainingSealedClass(
        val location: Location
    )

    @Serializable
    sealed class Location {

        @Serializable
        data object NewYork : Location()

        @Serializable
        data object Tokyo : Location()

        @Serializable
        sealed class EuropeanLocations : Location() {
            @Serializable
            data object London : EuropeanLocations()

            @Serializable
            data object Paris : EuropeanLocations()
        }
    }

    @Serializable
    data class NestedDataClass<T>(
        @Serializable
        val nested: NestedDataClass<T>? = null,
        @Serializable
        val value: T? = null
    )

    @Serializable(with = SealedGenericClassSerializer::class)
    sealed class SealedGenericClass<T> {
        @Serializable
        data class A<T>(val value: T) : SealedGenericClass<T>()

        @Serializable
        data class B<T>(val value: T) : SealedGenericClass<T>()
    }

    @Serializable(with = SealedGenericNestedClassSerializer::class)
    sealed class SealedGenericNestedClass<T> {
        @Serializable
        data class A<T>(val list: List<SealedGenericNestedClass<T>>) : SealedGenericNestedClass<T>()

        @Serializable
        data class B<T>(val value: T?) : SealedGenericNestedClass<T>()
    }
}

@OptIn(InternalSerializationApi::class)
class SealedGenericClassSerializer<T>(valueSerializer: KSerializer<T>): KSerializer<SealedGenericClass<T>> {
    private val serializer = SealedClassSerializer(
        SealedGenericClass::class.simpleName!!,
        SealedGenericClass::class,
        arrayOf(SealedGenericClass.A::class, SealedGenericClass.B::class),
        arrayOf(SealedGenericClass.A.serializer(valueSerializer), SealedGenericClass.B.serializer(valueSerializer))
    )

    override val descriptor: SerialDescriptor = serializer.descriptor
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): SealedGenericClass<T> { return serializer.deserialize(decoder) as SealedGenericClass<T> }
    override fun serialize(encoder: Encoder, value: SealedGenericClass<T>) { serializer.serialize(encoder, value) }
}

@OptIn(InternalSerializationApi::class)
class SealedGenericNestedClassSerializer<T>(valueSerializer: KSerializer<T>): KSerializer<SealedGenericNestedClass<T>> {
    private val serializer = SealedClassSerializer(
        SealedGenericNestedClass::class.simpleName!!,
        SealedGenericNestedClass::class,
        arrayOf(SealedGenericNestedClass.A::class, SealedGenericNestedClass.B::class),
        arrayOf(SealedGenericNestedClass.A.serializer(valueSerializer), SealedGenericNestedClass.B.serializer(valueSerializer))
    )

    override val descriptor: SerialDescriptor = serializer.descriptor
    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): SealedGenericNestedClass<T> { return serializer.deserialize(decoder) as SealedGenericNestedClass<T> }
    override fun serialize(encoder: Encoder, value: SealedGenericNestedClass<T>) { serializer.serialize(encoder, value) }
}
