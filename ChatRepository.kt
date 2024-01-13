class ChatRepository(
    private val apiService: ApiChatService

) : BaseApiResponse() {

    /**
     * Api call to fetch chat rooms.
     */
    suspend fun getChatRooms(request: GetRoomsRequest): Result<GetRoomsResponse> {
        val queryMap = HashMap<String, String>().apply {
            put("userid", request.userId)
            put("offset", request.offset.toString())
            put("pagesize", request.pageSize.toString())
            put("isadmin", request.isAdmin.toString())
            put("sortorder", request.sortorder.toString())
            put("sortby", request.sortby.toString())
        }

        return safeApiCall { apiService.getChatRooms(queryMap) }
    }

    /**
     * Api call to fetch chat room with a particular name.
     */
    suspend fun getRoomByName(roomName: String): Result<GetRoomsResponse> {
        val queryMap = HashMap<String, String>().apply {
            put("room", roomName)
            put("sortorder", "0")
            put("sortby", "3")
        }

        return safeApiCall { apiService.getRoomByName(queryMap) }
    }

    suspend fun getAllMessages(request: GetAllMessagesRequest): Result<GetAllMessagesResponse> {
        return safeApiCall { apiService.getAllMessages(request) }
    }

    suspend fun deleteMessage(request: DeleteMessageRequest): Result<DeleteMessageResponse> {
        val queryMap = HashMap<String, String>().apply {
            put("room", request.roomId.toString())
            put("msg", request.msgId.toString())
        }

        return safeApiCall { apiService.deleteMessage(queryMap) }
    }

    suspend fun getAllNewMessagesCount(userId: String): Result<GetNewMessagesCountResponse> {

        return safeApiCall { apiService.getAllNewMessagesCount(userId) }
    }

    suspend fun getAllRoomNewMessagesCount(
        request: GetRoomsUnreadMessagesCountRequest
    ): Result<GetNewRoomMessagesCountResponse> {

        return safeApiCall { apiService.getAllRoomNewMessagesCount(request) }
    }

    suspend fun getAllImageUrlList(
        request: GetImageUrlListRequest
    ): Result<GetAllImageUrlListResponse> {

        return safeApiCall { apiService.getAllImageUrlList(request) }
    }


    suspend fun getPreSignedUrl(imageId: String? = null): Result<GetPreSignedUrlResponse> {
        return imageId?.let {
            safeApiCall { apiService.getPreSignedDownloadUrl(imageId) }
        } ?: run {
            safeApiCall { apiService.getPreSignedUploadUrl() }
        }
    }

    suspend fun sendMessage(request: SendMessageRequest): Result<SendMessageResponse> {
        return safeApiCall { apiService.sendMessage(request) }
    }

    suspend fun sendMessageReaction(request: SendMessageReactionRequest): Result<SendMessageReactionResponse> {
        return safeApiCall { apiService.sendMessageReaction(request) }
    }

    suspend fun getMembersCount(roomId: Long): Result<GetMembersCountResponse> {
        return safeApiCall { apiService.getMembersCount(roomId) }
    }

    suspend fun getMembersByType(request: GetMembersByTypeRequest): Result<GetMembersResponse> {
        return safeApiCall { apiService.getMembersByType(request) }
    }

    suspend fun addRoom(request: AddRoomRequest): Result<AddRoomResponse> {
        return safeApiCall { apiService.addRoom(request) }
    }

    suspend fun addChatUsers(request: AddChatUserRequest): Result<AddChatUserResponse> {
        return safeApiCall { apiService.addChatUsers(request) }
    }


    suspend fun uploadAndSendMedia(
        request: SendMessageRequest,
        listener: ProgressCallback
    ): Result<SendMessageResponse> {
        val result = safeApiCall {
            apiService.getPreSignedUploadUrl()
        }

        when (result) {

            is Result.Success -> {
                val url = result.data!!.url
                val name = result.data.name

                val requestBody = createRequestBody(request.filePath, request.contentType)
                val progressBody = ProgressRequestBody(requestBody, listener)
                val uploadResult = apiService.uploadPreSignedUrlFile(url, progressBody, listener)

                return if (uploadResult.isSuccessful) {
                    request.filePath = name
                    val sendMessageResult = safeApiCall { apiService.sendMessage(request) }

                    if (sendMessageResult is Result.Success) {
                        sendMessageResult.data?.let {
                            it.filePath = name
                        }
                    }

                    sendMessageResult
                } else {
                    Result.Error(result.errorCode!!, result.message!!)
                }
            }

            else -> {
                return Result.Error(result.errorCode!!, result.message!!)
            }
        }
    }

    suspend fun uploadAndSendMediaForVideoDiary(
        request: UploadMediaForVideoDiaryRequest,
        listener: ProgressCallback
    ): Result<UploadRecordedMediaResponse> {
        val result = safeApiCall {
            apiService.getPreSignedUploadUrlForVideoDiary(true)
        }

        when (result) {

            is Result.Success -> {
                val url = result.data!!.url
                val name = result.data.name

                val requestBody = createRequestBody(request.filePath, request.contentType)
                val progressBody = ProgressRequestBody(requestBody, listener)
                val uploadResult = apiService.uploadPreSignedUrlFile(url, progressBody, listener)

                return if (uploadResult.isSuccessful) {

                    val response = UploadRecordedMediaResponse(
                        uploadResult.code(),
                        uploadResult.message(),
                        request.filePath,
                        name,
                        request.surveyId
                    )
                    Result.Success(response)

                } else {
                    Log.e("Uploaded", "Failed ")
                    Result.Error(result.errorCode!!, result.message!!)

                }
            }

            else -> {
                Log.e("Uploaded", "Failed : ")
                return Result.Error(result.errorCode!!, result.message!!)
            }
        }
    }

    suspend fun downloadMedia(request: DownloadMediaRequest): Result<DownloadMediaResponse> {
        val result = safeApiCall {
            apiService.getPreSignedDownloadUrl(request.mediaName)
        }

        when (result) {

            is Result.Success -> {
                val downloaded = download(result.data!!.url, request.destinationPath)
                return if (downloaded) {
                    val data =
                        DownloadMediaResponse(request.messageId.toString(), request.destinationPath)
                    Result.Success(data)
                } else {
                    Result.Error(1000, "Download failed")
                }
            }

            else -> {
                return Result.Error(result.errorCode!!, result.message!!)
            }
        }
    }

    fun downloadImage(request: DownloadImageRequest): Result<DownloadMediaResponse> {
        val downloaded = download(request.image, request.imageFilePath)
        return if (downloaded) {
            val data = DownloadMediaResponse(request.id, request.imageFilePath)
            Result.Success(data)
        } else {
            Result.Error(1000, "Download failed")
        }
    }

    fun downloadImages(request: List<DownloadImageRequest>): Result<List<DownloadMediaResponse>> {
        val resultList = mutableListOf<DownloadMediaResponse>()

        request.forEach {

            val downloaded = download(it.image, it.imageFilePath)
            if (downloaded) {
                resultList.add(
                    DownloadMediaResponse(
                        it.id,
                        it.imageFilePath
                    )
                )
            }
        }

        return Result.Success(resultList)
    }


    fun downloadChatRoomIcon(request: DownloadChatRoomIconRequest): Result<DownloadMediaResponse> {

        val downloaded = download(request.iconFilePath, request.destinationPath)
        return if (downloaded) {
            val data = DownloadMediaResponse(request.roomId.toString(), request.destinationPath)
            Result.Success(data)
        } else {
            Result.Error(1000, "Download failed")
        }

    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun download(urlString: String, destination: String): Boolean {
        try {
            val url = URL(urlString)
            val urlConnection = url.openConnection()
            urlConnection.connect()

            val inputStream = urlConnection.getInputStream()
            val fileOutputStream = FileOutputStream(destination)

            val buffer = ByteArray(1024)
            var bufferLength: Int

            while (inputStream.read(buffer).also { bufferLength = it } > 0) {
                fileOutputStream.write(buffer, 0, bufferLength)

                //add up the size so we know how much is downloaded
                // downloadedSize += bufferLength

                //this is where you would do something to report the prgress, like this maybe
                //updateProgress(downloadedSize, totalSize);
            }

            fileOutputStream.close()
            return true
        } catch (e: Exception) {
            return false
        }
    }


    private fun createRequestBody(filePath: String, mimeType: String): RequestBody {
        return File(filePath).asRequestBody(mimeType.toMediaTypeOrNull())
    }

    suspend fun getMessageById(messageId: Long, roomId: Long): Result<GetMessageByIdResponse> {
        return safeApiCall {
            apiService.getMessageById(messageId, roomId)
        }
    }
}