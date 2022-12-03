package co.early.persista

import co.early.fore.kt.core.delegate.Fore
import co.early.fore.kt.core.delegate.TestDelegateDefault
import co.early.fore.kt.core.logging.Logger
import co.early.fore.kt.core.logging.SystemLogger
import co.early.persista.TestState.*
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.Executors.newSingleThreadExecutor
import kotlin.reflect.typeOf

class PerSistaTest {

    @MockK
    private lateinit var mockLogger: Logger
    private lateinit var dataFolder: TemporaryFolder
    private val logger = SystemLogger()
    private val testDispatcher: CoroutineDispatcher = newSingleThreadExecutor().asCoroutineDispatcher()

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

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        Fore.setDelegate(TestDelegateDefault())
        dataFolder = TemporaryFolder()
        dataFolder.create()
    }

    @Test
    fun `when state is written, state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
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
    fun `when multiple states are written, multiple states are read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
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
    fun `when multiple states are written, one is cleared, other state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
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
        perSista.clear(testState1.javaClass.kotlin) {
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
    fun `when state is not written, default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
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
    fun `when state is over written, new state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
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
    fun `when state is written then everything wiped, default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
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
    fun `when state is written then cleared, default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        perSista.write(testState2) {
            logger.i("write response $it")
        }
        perSista.clear(testState2.javaClass.kotlin) {
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
    fun `when state is cleared without being written, default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
        var readResponse: DashboardState? = null

        logger.i("starting")

        // act
        perSista.clear(testState1.javaClass.kotlin) {
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
    fun `when state is written with suspending api, state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
        var writeResponse: DashboardState?
        var readResponse: DashboardState?

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
    fun `when state is not written with suspending api, default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
        var readResponse: DashboardState?

        logger.i("starting")

        // act
        runBlocking {
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when state is over written with suspending api, new state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
        var writeResponse: DashboardState?
        var readResponse: DashboardState?

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
    fun `when state is written then everything wiped with suspending api, default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
        var writeResponse: DashboardState?
        var readResponse: DashboardState?

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
    fun `when state is written then cleared with suspending api, default state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
        var writeResponse: DashboardState?
        var readResponse: DashboardState?

        logger.i("starting")

        // act
        runBlocking {
            writeResponse = perSista.write(testState2)
            perSista.clear(testState2.javaClass.kotlin)
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(testState2, writeResponse)
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when state is not written, with strict mode true, read does not throw exception`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder(), true)
        var readResponse: DashboardState?

        logger.i("starting")

        // act
        runBlocking {
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when state is not written, with strict mode true, error is logged on read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder(), true, mockLogger)
        var readResponse: DashboardState?

        logger.i("starting")

        // act
        runBlocking {
            readResponse = perSista.read(testState1)
        }

        // assert
        verify(exactly = 1) { mockLogger.e(any(), any<Throwable>()) }
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when writing non-serializable state, with strict mode false, no exception thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder(), false)
        var writeResponse: SomethingElseState?
        var readResponse: SomethingElseState?

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
    fun `when writing non-serializable state, with strict mode false, error is logged`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder(), false, mockLogger)

        logger.i("starting")

        // act
        runBlocking {
            perSista.write(testState4NonSerializable)
        }

        // assert
        verify(exactly = 1) { mockLogger.e(any(), any<Throwable>()) }
    }

    @Test
    fun `when clearing non-serializable state, with strict mode false, no exception thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder(), false)

        logger.i("starting")

        // act
        runBlocking {
            perSista.clear(testState4NonSerializable.javaClass.kotlin)
        }

        // assert
        assert(true)
    }

    @Test
    fun `when writing non-serializable state, with strict mode true, exception is thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder(), true)
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
        assertEquals(SerializationException::class.java, exception?.javaClass)
    }

    @Test
    fun `when reading non-serializable state, with strict mode false, no exception thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder(), false)
        var readResponse: DashboardState?

        logger.i("starting")

        // act
        runBlocking {
            readResponse = perSista.read(testState1)
        }

        // assert
        assertEquals(testState1, readResponse)
    }

    @Test
    fun `when reading non-serializable state, with strict mode false, error is logged`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder(), false, mockLogger)

        logger.i("starting")

        // act
        runBlocking {
            perSista.read(testState4NonSerializable)
        }

        // assert
        verify(exactly = 1) { mockLogger.e(any(), any<Throwable>()) }
    }

    @Test
    fun `when reading non-serializable state, with strict mode true, exception is thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder(), true)
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
        assertEquals(SerializationException::class.java, exception?.javaClass)
    }

    @Test
    fun `when state is written with manually specified type, state is read`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder())
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
    fun `when writing state with wrong type parameter, exception is thrown`() {

        // arrange
        val perSista = createPerSista(dataFolder.newFolder(), true)
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
        assertEquals(ClassCastException::class.java, exception?.javaClass)
    }

    private fun createPerSista(
        dataDirectory: File,
        strictMode: Boolean = false,
        testLogger: Logger = logger
    ): PerSista {
        return PerSista(
            dataDirectory = dataDirectory,
            mainDispatcher = testDispatcher,
            writeReadDispatcher = testDispatcher,
            logger = testLogger,
            strictMode = strictMode
        )
    }
}
