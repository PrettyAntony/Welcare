import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CareDocChatRepositoryTest {

    @get:Rule
    val mockApiRule = MockApiChatRule()

    private lateinit var careDocChatRepository: CareDocChatRepository

    @Before
    fun setUp() {
        careDocChatRepository = CareDocChatRepository(mockApiRule.apiService)
    }


    @Test
    fun getChatRooms_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(getChatRoomsResponseCodeJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = getChatRoomsResponseData
        val result = careDocChatRepository.getChatRooms(GetRoomsRequest("3018982832044245752"))

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun getChatRooms_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.getChatRooms(GetRoomsRequest("3018982832044245752"))

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun getChatRoomsByName_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(getChatRoomByNameResponseCodeJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = getChatRoomByNameData
        val result = careDocChatRepository.getRoomByName("pa-pa-3018982832044245752")

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun getChatRoomsByName_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.getRoomByName("pa-pa-3018982832044245752")

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun getAllMessages_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(getChatMessagesResponseCodeJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = getChatMessagesData
        val result = careDocChatRepository.getAllMessages(getAllMessagesRequest)

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun getAllMessages_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.getAllMessages(getAllMessagesRequest)

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun deleteMessage_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(deleteMessageResponseJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = deleteMessageResponseData
        val result = careDocChatRepository.deleteMessage(deleteMessageRequest)

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun deleteMessage_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.deleteMessage(deleteMessageRequest)

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun getAllUnreadMessagesCount_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(getNewMessagesCountResponseJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = getNewMessagesCountResponseData
        val result = careDocChatRepository.getAllNewMessagesCount("3012562229187838836")

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun getAllUnreadMessagesCount_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.getAllNewMessagesCount("3012562229187838836")

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun getRoomUnreadMessagesCount_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(getRoomUnreadMessagesCountResponseJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = getRoomsUnreadMessageResponseData
        val result =
            careDocChatRepository.getAllRoomNewMessagesCount(getAllRoomsUnreadMessagesCountRequest)

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun getRoomUnreadMessagesCount_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result =
            careDocChatRepository.getAllRoomNewMessagesCount(getAllRoomsUnreadMessagesCountRequest)

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun sendMessage_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(sendMessageResponseJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = sendMessageResponseData
        val result = careDocChatRepository.sendMessage(sendMessageRequest)

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun sendMessage_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.sendMessage(sendMessageRequest)

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun sendMessageReaction_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(sendMessageReactionResponseJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = sendMessageReactionResponseData
        val result = careDocChatRepository.sendMessageReaction(sendMessageReactionRequest)

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun sendMessageReaction_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.sendMessageReaction(sendMessageReactionRequest)

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun getMessageById_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(getMessageDetailsByIdResponseJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = getMessageByIdResponseData
        val result = careDocChatRepository.getMessageById(3048743041113260756, 3044524473962201595)

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun getMessageById_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.getMessageById(3048743041113260756, 3044524473962201595)

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun getMembersCount_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(getChatRoomUsersCountResponseJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = getChatRoomUsersCountData
        val result = careDocChatRepository.getMembersCount(3017642538946265107)

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun getMembersCount_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.getMembersCount(3017642538946265107)

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun getMemberByType_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(getMembersByTypeResponseCodeJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = getMembersByTypeData
        val result = careDocChatRepository.getMembersByType(
            GetMembersByTypeRequest(
                arrayListOf(4),
                3017642538946265107,
                20,
                0
            )
        )

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun getMemberByType_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.getMembersByType(
            GetMembersByTypeRequest(
                arrayListOf(4),
                3017642538946265107,
                20,
                0
            )
        )

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun addChatRoom_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(addChatRoomResponseJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = addChatRoomResponseData
        val result = careDocChatRepository.addRoom(addChatRoomRequest)

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun addChatRoom_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.addRoom(addChatRoomRequest)

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }

    @Test
    fun addChatUser_correctResponse_successResult() = runTest {

        val response = MockResponse()
            .setBody(addChatRoomUserResponseJson)
            .setResponseCode(200)

        mockApiRule.mockWebServer.enqueue(response)

        val expectedResponse = addChatRoomUserResponseData
        val result = careDocChatRepository.addChatUsers(addChatRoomUserRequest)

        // then
        Truth.assertThat(result.data).isEqualTo(expectedResponse)
    }

    @Test
    fun addChatUser_errorResponse_errorResult() = runTest {

        val response = MockResponse()
            .setBody(errorResponseJson)
            .setResponseCode(400)

        mockApiRule.mockWebServer.enqueue(response)

        val result = careDocChatRepository.addChatUsers(addChatRoomUserRequest)

        // then
        Truth.assertThat(result.errorCode).isEqualTo(400)
    }
}