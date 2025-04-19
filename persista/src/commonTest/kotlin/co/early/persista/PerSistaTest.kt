package co.early.persista

import MockLogger
import co.early.fore.core.delegate.Fore
import co.early.fore.core.delegate.TestDelegateDefault
import co.early.fore.core.logging.Logger
import co.early.fore.core.logging.SystemLogger
import co.early.persista.TestState.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

import kotlin.reflect.typeOf
import kotlin.test.BeforeTest
import kotlin.test.DefaultAsserter.assertNotNull
import kotlin.test.assertTrue

class PerSistaTest {

    private val dataFolder: Path = "test".toPath()
    private val logger = SystemLogger()
    private val mockLogger = MockLogger()

    @BeforeTest
    fun setup() {
        Fore.setDelegate(TestDelegateDefault())
        okio.FileSystem.SYSTEM.deleteRecursively(dataFolder)
        mockLogger.clear()
    }

    @Test
    fun `when state is written - state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var writeResponse: DashboardState? = null
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        perSista.write(testState1) {
            logger.i("write response: $it")
            writeResponse = it
        }
        perSista.read(testState2) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertEquals(testState1, writeResponse)
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when multiple states are written - multiple states are read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var readResponseDashBoard: DashboardState? = null
        var readResponseMoreState: MoreState? = null

        logger.i("starting")

        // act
        perSista.write(testState1) {
            logger.i("write response a : $it")
        }
        perSista.write(testState6) {
            logger.i("write response b : $it")
        }
        perSista.read(testState2) {
            logger.i("read response a $it")
            readResponseDashBoard = it
        }
        perSista.read(MoreState()) {
            logger.i("read response b $it")
            readResponseMoreState = it
        }

        // assert
        assertEquals(testState1, readResponseDashBoard)
        assertEquals(testState6, readResponseMoreState)
    }

    @Test
    fun `when multiple states are written - one is cleared and other state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var readResponseDashBoard: DashboardState? = null
        var readResponseMoreState: MoreState? = null

        logger.i("starting")

        // act
        perSista.write(testState1) {
            logger.i("write response a : $it")
        }
        perSista.write(testState6) {
            logger.i("write response b : $it")
        }
        perSista.clear(testState1::class) {
            logger.i("cleared a")
        }
        perSista.read(testState2) {
            logger.i("read response a $it")
            readResponseDashBoard = it
        }
        perSista.read(MoreState()) {
            logger.i("read response b $it")
            readResponseMoreState = it
        }

        // assert
        assertEquals(testState2, readResponseDashBoard)
        assertEquals(testState6, readResponseMoreState)
    }

    @Test
    fun `when state is not written - default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        perSista.read(testState2) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertEquals(testState2, readResponse)
    }

    @Test
    fun `when state is over written - new state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        perSista.write(testState1) {
            logger.i("write response $it")
        }
        perSista.write(testState2) {
            logger.i("write response $it")
        }
        perSista.write(testState3) {
            logger.i("write response $it")
        }
        perSista.read(testState1) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertEquals(testState3, readResponse)
    }

    @Test
    fun `when state is written then everything wiped - default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        perSista.write(testState2) {
            logger.i("write response $it")
        }
        perSista.wipeEverything {
            logger.i("wipeEverything complete")
        }
        perSista.read(testState1) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when state is written then cleared - default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        perSista.write(testState2) {
            logger.i("write response $it")
        }
        perSista.clear(testState2::class) {
            logger.i("clear complete")
        }
        perSista.read(testState1) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when state is cleared without being written - default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        perSista.clear(testState1::class) {
            logger.i("clear complete")
        }
        perSista.read(testState1) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when state is written with suspending api - state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var writeResponse: DashboardState? = null
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        runBlocking {
            writeResponse = perSista.write(testState1)
            readResponse = perSista.read(testState2)
        }

        // assert
        assertEquals(testState1, writeResponse)
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when state is not written with suspending api - default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        runBlocking {
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when state is over written with suspending api - new state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var writeResponse: DashboardState? = null
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        runBlocking {
            writeResponse = perSista.write(testState1)
            writeResponse = perSista.write(testState2)
            writeResponse = perSista.write(testState3)
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(testState3, writeResponse)
        assertEquals(testState3, readResponse)
    }

    @Test
    fun `when state is written then everything wiped with suspending api - default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var writeResponse: DashboardState? = null
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        runBlocking {
            writeResponse = perSista.write(testState2)
            perSista.wipeEverything()
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(testState2, writeResponse)
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when state is written then cleared with suspending api - default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var writeResponse: DashboardState? = null
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        runBlocking {
            writeResponse = perSista.write(testState2)
            perSista.clear(testState2::class)
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(testState2, writeResponse)
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `given strict mode true - when state is not written - read does not throw exception`() {

        // arrange
        val perSista = createPerSista(dataFolder, true)
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        runBlocking {
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `given strict mode true - when state is not written - warning is logged on read`() {

        // arrange
        val perSista = createPerSista(dataFolder, true, mockLogger)
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        runBlocking {
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(2, mockLogger.callHistory.size)
        assertEquals(MockLogger.Level.W, mockLogger.callHistory[1].level)
        assertNotNull("should have message", mockLogger.callHistory[1].message)
        assertNotNull("should have throwable", mockLogger.callHistory[1].throwable)
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `given strict mode false - when writing non-serializable state - no exception thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder, false)
        var writeResponse: SomethingElseState? = null
        var readResponse: SomethingElseState? = null

        logger.i("starting")

        // act
        runBlocking {
            writeResponse = perSista.write(testState4NonSerializable)
        }
        runBlocking {
            readResponse = perSista.write(testState5NonSerializable)
        }

        // assert
        assertEquals(testState4NonSerializable, writeResponse)
        assertEquals(testState5NonSerializable, readResponse)
    }

    @Test
    fun `given strict mode false - when writing non-serializable state - error is logged`() {

        // arrange
        val perSista = createPerSista(dataFolder, false, mockLogger)

        logger.i("starting")

        // act
        runBlocking {
            perSista.write(testState4NonSerializable)
        }

        // assert
        assertEquals(1, mockLogger.callHistory.size)
        assertEquals(MockLogger.Level.E, mockLogger.callHistory[0].level)
        assertNotNull("should have message", mockLogger.callHistory[0].message)
        assertNotNull("should have throwable", mockLogger.callHistory[0].throwable)
    }

    @Test
    fun `given strict mode false - when clearing non-serializable state - no exception thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder, false)

        logger.i("starting")

        // act
        runBlocking {
            perSista.clear(testState4NonSerializable::class)
        }

        // assert
        assertTrue(true)
    }

    @Test
    fun `given strict mode true - when writing non-serializable state - exception is thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder, true)
        var exception: Exception? = null

        logger.i("starting")

        // act
        try {
            runBlocking {
                perSista.write(testState4NonSerializable)
            }
        } catch (e: Exception) {
            exception = e
        }

        // assert
        assertTrue(exception is SerializationException, "expected SerializationException, got $exception")
    }

    @Test
    fun `given strict mode false - when reading non-serializable state - no exception thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder, false)
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        runBlocking {
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `given strict mode false - when reading non-serializable state - error is logged`() {

        // arrange
        val perSista = createPerSista(dataFolder, false, mockLogger)

        logger.i("starting")

        // act
        runBlocking {
            perSista.read(testState4NonSerializable)
        }

        // assert
        assertEquals(1, mockLogger.callHistory.size)
        assertEquals(MockLogger.Level.E, mockLogger.callHistory[0].level)
        assertNotNull("should have message", mockLogger.callHistory[0].message)
        assertNotNull("should have throwable", mockLogger.callHistory[0].throwable)
    }

    @Test
    fun `given strict mode true - when reading non-serializable state - exception is thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder, true)
        var exception: Exception? = null

        logger.i("starting")

        // act
        try {
            runBlocking {
                perSista.read(testState4NonSerializable)
            }
        } catch (e: Exception) {
            exception = e
        }

        // assert
        assertTrue(exception is SerializationException, "expected SerializationException, got $exception")
    }

    @Test
    fun `when state is written with manually specified type - state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var writeResponse: DashboardState? = null
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        perSista.write(testState1, typeOf<DashboardState>()) {
            logger.i("write response: $it")
            writeResponse = it
        }
        perSista.read(testState2, typeOf<DashboardState>()) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertEquals(testState1, writeResponse)
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when state is written with manually specified type - which contains a sealed class - state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var firstReadResponse: StateContainingSealedClass? = null
        var writeResponse: StateContainingSealedClass? = null
        var readResponse: StateContainingSealedClass? = null

        logger.i("starting")

        // act
        perSista.read(testState8, typeOf<StateContainingSealedClass>()) {
            logger.i("first read response $it")
            firstReadResponse = it
        }
        perSista.write(testState7, typeOf<StateContainingSealedClass>()) {
            logger.i("write response: $it")
            writeResponse = it
        }
        perSista.read(testState8, typeOf<StateContainingSealedClass>()) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertEquals(testState8, firstReadResponse)
        assertEquals(testState7, writeResponse)
        assertEquals(testState7, readResponse)
    }

    @Test
    fun `when state is written with manually specified type - which is a generic nested data class - state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var firstReadResponse: NestedDataClass<String>? = null
        var writeResponse: NestedDataClass<String>? = null
        var readResponse: NestedDataClass<String>? = null

        logger.i("starting")

        // act
        perSista.read(testState10, typeOf<NestedDataClass<String>>()) {
            logger.i("first read response $it")
            firstReadResponse = it
        }
        perSista.write(testState9, typeOf<NestedDataClass<String>>()) {
            logger.i("write response: $it")
            writeResponse = it
        }
        perSista.read(testState10, typeOf<NestedDataClass<String>>()) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertNotEquals(testState10, testState9)
        assertEquals(testState10, firstReadResponse)
        assertEquals(testState9, writeResponse)
        assertEquals(testState9, readResponse)
    }

    @Test
    fun `when state is written with manually specified type - which is a generic sealed class - state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var firstReadResponse: SealedGenericClass<Location>? = null
        var writeResponse: SealedGenericClass<Location>? = null
        var readResponse: SealedGenericClass<Location>? = null

        logger.i("starting")

        // act
        perSista.read(testState12, typeOf<SealedGenericClass<Location>>()) {
            logger.i("first read response $it")
            firstReadResponse = it
        }
        perSista.write(testState11, typeOf<SealedGenericClass<Location>>()) {
            logger.i("write response: $it")
            writeResponse = it
        }
        perSista.read(testState12, typeOf<SealedGenericClass<Location>>()) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertNotEquals(testState11, testState12)
        assertEquals(testState12, firstReadResponse)
        assertEquals(testState11, writeResponse)
        assertEquals(testState11, readResponse)
    }

    @Test
    fun `when state is written with manually specified type - which is a generic sealed and nested class - state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder)
        var firstReadResponse: SealedGenericNestedClass<Location>? = null
        var writeResponse: SealedGenericNestedClass<Location>? = null
        var readResponse: SealedGenericNestedClass<Location>? = null

        logger.i("starting")

        // act
        perSista.read(testState14, typeOf<SealedGenericNestedClass<Location>>()) {
            logger.i("first read response $it")
            firstReadResponse = it
        }
        perSista.write(testState13, typeOf<SealedGenericNestedClass<Location>>()) {
            logger.i("write response: $it")
            writeResponse = it
        }
        perSista.read(testState14, typeOf<SealedGenericNestedClass<Location>>()) {
            logger.i("read response $it")
            readResponse = it
        }

        // assert
        assertNotEquals(testState13, testState14)
        assertEquals(testState14, firstReadResponse)
        assertEquals(testState13, writeResponse)
        assertEquals(testState13, readResponse)
    }

    @Test
    fun `when writing state with wrong type parameter - exception is thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder, true)
        var exception: Exception? = null

        logger.i("starting")

        // act
        try {
            perSista.write(testState4NonSerializable, typeOf<Driver>()) {
                logger.i("write response: $it")
            }
        } catch (e: Exception) {
            exception = e
        }

        // assert
        assertTrue(exception is ClassCastException, "expected ClassCastException, got $exception")
    }

    private fun createPerSista(
        dataDirectory: Path,
        strictMode: Boolean = false,
        testLogger: Logger = logger
    ): PerSista {
        return PerSista(
            dataPath = dataDirectory,
            logger = testLogger,
            strictMode = strictMode,
        )
    }

    private val testState1 = DashboardState(
        dashboardId = 1,
        userName = "erdo"
    )
    private val testState2 = DashboardState(
        dashboardId = 2,
        userName = "odre",
        drivers = listOf(
            Driver(
                driverId = 99,
                driverName = "francis",
                powerLevel = 100,
                lat = 123,
                long = 456,
            )
        )
    )
    private val testState3 = DashboardState(
        dashboardId = 3,
        userName = "uName",
        error = Error.SECURITY
    )
    private val testState4NonSerializable = SomethingElseState(1, "user1")
    private val testState5NonSerializable = SomethingElseState(2, "user2")
    private val testState6 = MoreState(true)

    private val testState7 = StateContainingSealedClass(Location.EuropeanLocations.London)
    private val testState8 = StateContainingSealedClass(Location.NewYork)

    private val testState9 = NestedDataClass(
        nested = NestedDataClass(
            nested = NestedDataClass(
                nested = null,
                value = "A"
            )
        )
    )
    private val testState10 = NestedDataClass(
        nested = NestedDataClass(
            nested = NestedDataClass(
                nested = null,
                value = "B"
            )
        )
    )

    private val testState11: SealedGenericClass<Location> =
        SealedGenericClass.A(Location.EuropeanLocations.Paris)
    private val testState12: SealedGenericClass<Location> = SealedGenericClass.B(Location.Tokyo)

    private val testState13: SealedGenericNestedClass<Location> = SealedGenericNestedClass.A(
        listOf(
            SealedGenericNestedClass.B(Location.NewYork),
            SealedGenericNestedClass.B(Location.Tokyo),
            SealedGenericNestedClass.B(Location.EuropeanLocations.Paris),
        )
    )
    private val testState14: SealedGenericNestedClass<Location> =
        SealedGenericNestedClass.B(Location.EuropeanLocations.London)
}
