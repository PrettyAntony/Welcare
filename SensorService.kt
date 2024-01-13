

@AndroidEntryPoint
class SensorService : Service(), SensorListener {

    // Timer class used to create an alarm to send the stored sensor data to server
    private lateinit var timer: Timer

    private lateinit var gson: Gson

    // Checking the uploading is in progress or not
    private var isUploadingData = false

    @Inject
    lateinit var localDataSource: SensorDataLocalDataSource

    @Inject
    lateinit var remoteDataSource: SensorDataRemoteDataSource

    @Inject
    lateinit var appEventsDataSource: AppEventsDataSource

    @Inject
    lateinit var externalScope: CoroutineScope

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // Making this service as foreground service by showing a ongoing notification
        startForeground(NOTIFICATION_ID, generateNotification())

        LogUtil.d("SensorService: onCreate()")
        makeLogStatement("Sensor service created")

        externalScope.launch {
            appEventsDataSource.insertAppEvent(
                "Sensor service created",
                System.currentTimeMillis().toString()
            )
        }

        // Initializing timer
        timer = Timer(this)

        // Creating gson object
        gson = Gson()

        // Registering for broadcast receivers
        registerListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        makeLogStatement("Service started!")

        // Setting up all the listeners
        doSetup()
        return START_STICKY
    }

    override fun onSensorChanged(sensorEventData: SensorEventData) {
        /*LogUtil.e(
            TAG,
            "Sensor value: ${SensorType.fromInt(sensorEventData.sensorType)} : ${sensorEventData.sensorData}"
        )*/

        externalScope.launch(Dispatchers.IO) {
            localDataSource.insertSensorData(
                sensorEventData.sensorType,
                sensorEventData.sensorData,
                sensorEventData.date
            )
        }
    }


    /**
     * Creating notification object.
     */
    private fun generateNotification(): Notification {
        LogUtil.d(TAG + "generateNotification()")

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                NOTIFICATION_MESSAGE,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannel(notificationChannel)
        }

        return NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_MESSAGE)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round))
            .setSmallIcon(R.drawable.notification_icon)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .build()
    }

    /**
     *
     */
    private fun registerListener() {
        val intentFilter = IntentFilter().apply {
            addAction(getString(R.string.upload_data_files_intent))
        }

        registerReceiver(timerReceiver, intentFilter)
    }

    /**
     *
     */
    private fun doSetup() {
        // If accelerometer data tracking is enabled then,
        if (PersistentData.getAccelerometerEnabled()) {
            AccelerometerListener.getInstance(applicationContext, this).startListener()
        } else {
            AccelerometerListener.getInstance(applicationContext, this).turnOff(true)
        }

        // If Gyroscope data tracking is enabled then,
        if (PersistentData.getGyroscopeEnabled()) {
            GyroscopeListener.getInstance(applicationContext, this).startListener()
        } else {
            GyroscopeListener.getInstance(applicationContext, this).turnOff(true)
        }

        // If Magnetometer data tracking is enabled then,
        if (PersistentData.getMagnetoEnabled()) {
            MagnetometerListener.getInstance(applicationContext, this).startListener()
        }
        else {
            MagnetometerListener.getInstance(applicationContext, this).turnOff(true)
        }

        // If Proximity data tracking is enabled then,
        if (PersistentData.getProximityEnabled()) {
            ProximityListener.getInstance(applicationContext, this).turnOn()
        }
        else {
            ProximityListener.getInstance(applicationContext, this).turnOff()
        }

        // If Power state tracking is enabled then,
        if (PersistentData.getPowerStateEnabled()) {
            PowerStateListener.getInstance(applicationContext, this).turnOn()
        }
        else {
            PowerStateListener.getInstance(applicationContext, this).turnOff()
        }

        // If GPS tracking is enabled then,
        if (PersistentData.getGpsEnabled()) {
            GpsListener.getInstance(applicationContext, this).startListener()
        } else {
            GpsListener.getInstance(applicationContext, this).turnOff(true)
        }

        // If Wifi tracking is enabled then,
        if (PersistentData.getWifiEnabled()) {
            WifiListener.getInstance(applicationContext, this).turnOn()
        }
        else {
            WifiListener.getInstance(applicationContext, this).turnOff()
        }

        // If Reachability tracking is enabled then,
        if (PersistentData.getNetworkEnabled()) {
            NetworkListener.getInstance(applicationContext, this).turnOn()
        }
        else {
            NetworkListener.getInstance(applicationContext, this).turnOff()
        }

        // If Call tracking is enabled then,
        if (PersistentData.getCallsEnabled()) {
            CallLogger.getInstance(applicationContext, this).turnOn()
        } else {
            CallLogger.getInstance(applicationContext, this).turnOff()
        }

        // If Texts tracking is enabled then,
        if (PersistentData.getTextsEnabled()) {
            SmsReceivedListener.getInstance(applicationContext, this).turnOn()
            SmsSentLogger.getInstance(applicationContext, this).turnOn()
            MMSSentLogger.getInstance(applicationContext, this).turnOn()
        } else {
            SmsReceivedListener.getInstance(applicationContext, this).turnOff()
            SmsSentLogger.getInstance(applicationContext, this).turnOff()
            MMSSentLogger.getInstance(applicationContext, this).turnOff()
        }

        // Start timer to update sensor data to server in every 10 minutes
        startUploadDataTimer()
    }

    /**
     * Creating an alarm manager to start alarm after 10 minutes
     */
    private fun startUploadDataTimer() {
        timer.setupExactSingleAlarm(
            PersistentData.getUploadDataFilesFrequencyMilliseconds(),
            Timer.uploadDatafilesIntent
        )
    }

    /**
     * Syncing data to server every 10 minutes with 2 Apis for 5 minutes data.
     * For the first time lastUploadedTime will be zero so we use the timestamp of first record from db as lastUploadedTime.
     * If there is no records in db then current time will be set as lastUploadedTime.
     * Identifiers data will be sent only once.
     * After the successful api response the uploaded data will be deleted from db
     */
    private fun uploadSensorData() {
        externalScope.launch(Dispatchers.IO) {
            var needToUploadIdentifiersData = false

            var lastUpdatedDataTime = AppState.sensorLastUploadedTime
            val uploadEndTime = AppState.sensorUploadEndTime

            if (lastUpdatedDataTime == 0L) {
                needToUploadIdentifiersData = PersistentData.getIdentifiersEnabled()

                val firstItemDate =
                    localDataSource.getFirstSensorDataTime(Date(lastUpdatedDataTime))

                // First Item date is null means there are no items to be uploaded other than identifiers
                firstItemDate?.let {
                    lastUpdatedDataTime = it.time
                }
            }

            val sensorDataToBeUploadedList = mutableListOf<SensorDataModel>()
            sensorDataToBeUploadedList.clear()

            if (lastUpdatedDataTime == 0L) {
                if (needToUploadIdentifiersData) {
                    DeviceInfo.initialize(this@SensorService)
                    sensorDataToBeUploadedList.add(
                        SensorDataModel(
                            System.currentTimeMillis(),
                            "identifiers",
                            getIdentifierData()
                        )
                    )

                    startUpload(sensorDataToBeUploadedList, lastUpdatedDataTime, uploadEndTime)
                }
                else {
                    isUploadingData = false
                }

                AppState.sensorLastUploadedTime = uploadEndTime
            }
            else {
                var nextSyncTime = lastUpdatedDataTime + UPLOAD_INTERVAL

                LogUtil.e(TAG + "lastUploadedTime: $lastUpdatedDataTime")
                LogUtil.e(TAG + "uploadEndTime: $uploadEndTime")
                LogUtil.e(TAG + "nextSyncTime: $nextSyncTime")

                if (nextSyncTime >= uploadEndTime) {
                    nextSyncTime = uploadEndTime
                }

                val sensorDataList = localDataSource.getSensorDataList(lastUpdatedDataTime, nextSyncTime)
                LogUtil.e(TAG + "SensorDataListCount: ${sensorDataList.size}")

                if (needToUploadIdentifiersData) {
                    DeviceInfo.initialize(this@SensorService)
                    sensorDataToBeUploadedList.add(
                        SensorDataModel(
                            System.currentTimeMillis(),
                            "identifiers",
                            getIdentifierData()
                        )
                    )
                }

                sensorDataList.forEach { sensorEventData ->

                    when (sensorEventData.sensorType) {

                        SensorType.ACCELEROMETER.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "accelerometer",
                                getAccelerometerData(sensorEventData.sensorData)
                            )
                        )

                        SensorType.CALLS.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "calls",
                                getCallLogData(sensorEventData.sensorData)
                            )
                        )

                        SensorType.GPS.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "gps",
                                getGpsData(sensorEventData.sensorData)
                            )
                        )

                        SensorType.POWER_STATE.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "powerstate",
                                getPowerStateData(sensorEventData.sensorData)
                            )
                        )

                        SensorType.TEXTS.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "texts",
                                getTextLogData(sensorEventData.sensorData)
                            )
                        )

                        SensorType.WIFI.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "wifi",
                                getWifiData(sensorEventData.sensorData)
                            )
                        )

                        SensorType.PROXIMITY.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "proximity",
                                getProximityData(sensorEventData.sensorData)
                            )
                        )

                        SensorType.GYROSCOPE.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "gyro",
                                getGyroscopeData(sensorEventData.sensorData)
                            )
                        )

                        SensorType.MAGNETOMETER.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "magnetometer",
                                getMagnetoMeterData(sensorEventData.sensorData)
                            )
                        )

                        SensorType.REACHABILITY.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "reachability",
                                getReachabilityData(sensorEventData.sensorData)
                            )
                        )

                        SensorType.APP_LOG.value -> sensorDataToBeUploadedList.add(
                            SensorDataModel(
                                sensorEventData.date.time,
                                "applog",
                                getAppLogData(sensorEventData.sensorData)
                            )
                        )
                    }
                }

                if (sensorDataToBeUploadedList.isNotEmpty()) {
                    startUpload(sensorDataToBeUploadedList, nextSyncTime, uploadEndTime)
                } else if (nextSyncTime < uploadEndTime) {
                    AppState.sensorLastUploadedTime = nextSyncTime
                    uploadSensorData()
                }
                else {
                    isUploadingData = false
                }
            }
        }
    }

    /**
     * Calls api to upload data.
     * @param list the data needs to be uploaded
     * @param timeStamp the time at which the records are taken from db.
     * @param endTimeStamp the current time
     */
    private suspend fun startUpload(
        list: List<SensorDataModel>,
        timeStamp: Long,
        endTimeStamp: Long
    ) {
        val result = remoteDataSource.uploadSensorData(list)
        if (result is NetworkResult.Success) {
            val rowCount = localDataSource.deleteSensorDataList(timeStamp)
            LogUtil.e(TAG + "DeleteRowCount: $rowCount")

            AppState.sensorLastUploadedTime = timeStamp
            if (timeStamp != 0L && timeStamp < endTimeStamp) {
                uploadSensorData()
            }
            else {
                isUploadingData = false
            }
        }
        else {
            isUploadingData = false
        }
    }



    private fun sendEventDataDetails() {
        externalScope.launch {
            val list = appEventsDataSource.getEventDataList()

            if (list.isNotEmpty()) {
                WorkManagerHelper.addSendMetadataToWorkManagerQueue(list)
                appEventsDataSource.deleteEventDataList()
            }
        }
    }


    private fun getIdentifierData(): HashMap<String, String> {
        return HashMap<String, String>().apply {
            put("mac", DeviceInfo.getBluetoothMAC())
            put("phone_number", DeviceInfo.getPhoneNumber(this@SensorService))
            put("device_id", DeviceInfo.getAndroidID())
            put("device_os", "Android")
            put("os_version", DeviceInfo.getAndroidVersion())
            put("product", DeviceInfo.getProduct())
            put("brand", DeviceInfo.getBrand())
            put("hardware_id", DeviceInfo.getHardwareId())
            put("manufacturer", DeviceInfo.getManufacturer())
            put("model", DeviceInfo.getModel())
        }
    }

    /**
     * Format,
     * 0 -> accuracy,
     * 1 -> x,
     * 2 -> y,
     * 3 -> z
     */
    private fun getAccelerometerData(sensorData: String): HashMap<String, String> {
        val dataList = sensorData.split(",")
        return HashMap<String, String>().apply {
            put("accuracy", dataList[0])
            put("x", dataList[1])
            put("y", dataList[2])
            put("z", dataList[3])
        }
    }

    /**
     * Format,
     * 0 -> hashed phone number,
     * 1 -> call type,
     * 2 -> call timestamp,
     * 3 -> duration in seconds
     */
    private fun getCallLogData(sensorData: String): HashMap<String, String> {
        val dataList = sensorData.split(",")
        return HashMap<String, String>().apply {
            put("hashed_phone_number", dataList[0])
            put("call_type", dataList[1])
            put("duration_in_seconds", dataList[3])
        }
    }

    /**
     * Format,
     * 0 -> Latitude,
     * 1 -> Longitude,
     * 2 -> Altitude,
     * 3 -> Accuracy
     */
    private fun getGpsData(sensorData: String): HashMap<String, String> {
        val dataList = sensorData.split(",")
        return HashMap<String, String>().apply {
            put("latitude", dataList[0])
            put("longitude", dataList[1])
            put("altitude", dataList[2])
            put("accuracy", dataList[3])
        }
    }

    private fun getPowerStateData(sensorData: String): HashMap<String, String> {
        return HashMap<String, String>().apply {
            put("event", sensorData)
            put("level", "0.0")
        }
    }

    /**
     * Format,
     * timestamp, hashed phone number, sent vs received, message length, time sent
     * 0 -> Hashed phone number,
     * 1 -> Sent vs Received,
     * 2 -> Message length
     * 3 -> Time sent
     */
    private fun getTextLogData(sensorData: String): HashMap<String, String> {
        val dataList = sensorData.split(",")
        return HashMap<String, String>().apply {
            put("hashed_phone_number", dataList[0])
            put("sent_vs_received", dataList[1])
            put("message_length", dataList[2])
            put("time_sent", dataList[3])
        }
    }

    /**
     * Format,
     * timestamp, hashed phone number, sent vs received, message length, time sent
     * 0 -> Hashed mac,
     * 1 -> Frequency,
     * 2 -> Level
     */
    private fun getWifiData(sensorData: String): HashMap<String, String> {
        val dataList = sensorData.split(",")
        return HashMap<String, String>().apply {
            put("hashed_mac", dataList[0])
            put("frequency", dataList[1])
            put("rssi", dataList[2])
        }
    }

    private fun getProximityData(sensorData: String): HashMap<String, String> {
        return HashMap<String, String>().apply {
            put("event", sensorData)
        }
    }

    /**
     * Format,
     * 0 -> accuracy,
     * 1 -> x,
     * 2 -> y,
     * 3 -> z
     */
    private fun getGyroscopeData(sensorData: String): HashMap<String, String> {
        val dataList = sensorData.split(",")
        return HashMap<String, String>().apply {
            put("x", dataList[1])
            put("y", dataList[2])
            put("z", dataList[3])
        }
    }

    /**
     * Format,
     * 0 -> accuracy,
     * 1 -> x,
     * 2 -> y,
     * 3 -> z
     */
    private fun getMagnetoMeterData(sensorData: String): HashMap<String, String> {
        val dataList = sensorData.split(",")
        return HashMap<String, String>().apply {
            put("x", dataList[1])
            put("y", dataList[2])
            put("z", dataList[3])
        }
    }

    private fun getReachabilityData(sensorData: String): HashMap<String, String> {
        return HashMap<String, String>().apply {
            put("event", sensorData)
        }
    }

    private fun getAppLogData(sensorData: String): HashMap<String, String> {
        return HashMap<String, String>().apply {
            put("event", sensorData)
        }
    }

    private fun makeLogStatement(log: String) {
        onSensorChanged(
            SensorEventData(
                SensorType.APP_LOG.value,
                log,
                Date()
            )
        )
    }

    private val timerReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {

                getString(R.string.upload_data_files_intent) -> {
                    // Setting an alarm to send data to server.
                    startUploadDataTimer()

                    // Send app events.
                    sendEventDataDetails()

                    // Uploading sensor data from db to server
                    if (!isUploadingData) {
                        isUploadingData = true

                        AppState.sensorUploadEndTime = System.currentTimeMillis()
                        uploadSensorData()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        makeLogStatement("Sensor service stopped.")
        unregisterReceiver(timerReceiver)
        super.onDestroy()
    }

    override fun onLowMemory() {
        makeLogStatement("onLowMemory called.")
        super.onLowMemory()
    }

    companion object {
        private const val TAG = "SensorService"

        

        private const val NOTIFICATION_CHANNEL = "channel_sensor"

        private const val NOTIFICATION_TITLE = "Service"

        private const val NOTIFICATION_MESSAGE = "Active Data Collection"

        private const val UPLOAD_INTERVAL = 3 * 60 * 1000  // 3 minutes
    }
}