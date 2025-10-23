package com.example.airqualitytracker

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hivemq.client.mqtt.MqttClient
//added for push notifications
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object MqttManager {
    private val _mqttMessage = MutableLiveData<Pair<String, String>>() // topic to message
    val mqttMessage: LiveData<Pair<String, String>> = _mqttMessage
    private val client = MqttClient.builder()
        .useMqttVersion3()
        .serverHost("f8ff6f7472044412970284fc1b7b152c.s1.eu.hivemq.cloud")
        .serverPort(8883)
        .sslWithDefaultConfig()
        .buildAsync()

    fun connect(username: String, password: String, topic: String,onResult: (Boolean) -> Unit) {//Connects to the server cluster on hive mq.  Adding boolean to check flag for app to move forward
        var currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        Log.d("MQTT","Trying to connect at $currentTime")
        client.connectWith()
            .simpleAuth()
            .username(username)
            .password(password.toByteArray())
            .applySimpleAuth()
            .send()
            .whenComplete { connAck, throwable ->
                if (throwable != null) {
                    currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                    Log.d("MQTT", "Connection failed at $currentTime: ${throwable.message}")
                } else {
                    println("MQTT Connected successfully!")
                    subscribe(topic)
                    onResult(true)
                }
            }
    }

    private fun subscribe(topic: String) {
        client.subscribeWith()
            .topicFilter(topic)
            .callback { publish ->
                try {
                    val receivedTopic = publish.topic.toString()
                    val message = publish.payload
                        .map { buffer ->
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            String(bytes)
                        }.orElse("No payload")

                    //Log.d("MQTT", "Received on [$receivedTopic]: $message")//troubleshooting code for receiving messages from HiveMQ

                    // Post to LiveData (safe for UI)
                    _mqttMessage.postValue(Pair(receivedTopic, message))

                } catch (e: Exception) {
                    Log.e("MQTT", "Error while processing message: ${e.message}")//(4/1/2025)there was bug in code the would crash the app
                }
            }
            .send()
    }
    fun handleTemperatureUpdate(context: Context, temperatureString: MutableState<String>) {
        val temp = temperatureString.value.toFloatOrNull()
        val hasPermission =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        /*if (temp != null && temp < 5 && hasPermission) {
            showSystemNotification(context, "Low Temperature Alert", "Temperature dropped below 5°C")
        }*/
    }
    fun disconnect() {
        if (client.state.isConnected) {//checks for connection of MQTT to the broker
            client.disconnect()//disconnects the MQTT Broker from the app
                .whenComplete { _, throwable ->
                    if (throwable != null) {//debug checks
                        Log.e("MQTT", "Disconnect failed: ${throwable.message}")
                    } else {
                        Log.d("MQTT", "Disconnected successfully")
                    }
                }
        } else {
            Log.d("MQTT", "Client already disconnected")
        }
    }
    fun publish(topic: String, message: String) {//for publishing the switch data
        if (client.state.isConnected) {
            client.publishWith()//mqtt library
                .topic(topic)
                .payload(message.toByteArray())
                .qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e("MQTT", "Failed to publish: ${throwable.message}")
                    } else {
                        Log.d("MQTT", "Message published to $topic: $message")
                    }
                }
        } else {
            Log.e("MQTT", "Client is not connected, can't publish.")
        }
    }
    //New Listen Code that only listens to JSON Format
    fun startListening(
        temperatureState: MutableState<String>,
        humidityState: MutableState<String>,
        particleState: MutableState<String>,
        TVOCState: MutableState<String>,
        COState:MutableState<String>,
        jsonState: MutableState<String>
    ) {
        // Subscribe ONLY to JSON topic
        client.subscribeWith()
            .topicFilter("sensor/json")
            .callback { publish ->
                val topic = publish.topic.toString()
                val message = publish.payload
                    .map { buffer ->
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        String(bytes)
                    }.orElse("No payload")

                // Keep a copy of the raw JSON if you want to show/debug it
                jsonState.value = message

                // Parse and fan-out into UI states
                val env = TelemetryParser.parse(message)
                if (env == null) {
                    Log.e("MQTT", "JSON parse failed for: $message")
                    return@callback
                }

                val b = env.body
                // Map FlammableGases -> particleState as before
                temperatureState.value = b.Temperature.toString()
                humidityState.value    = b.Humidity.toString()
                particleState.value    = b.FlammableGases.toString()
                TVOCState.value        = b.TVOC.toString()
                COState.value          = b.CO.toString()

                // Optional logging
                TelemetryParser.parseEnqueuedInstant(env.enqueuedTime)?.let { ts ->
                    Log.d("MQTT", "Parsed JSON @ $ts -> T=${b.Temperature}, H=${b.Humidity}, Part=${b.FlammableGases}, TVOC=${b.TVOC}, CO=${b.CO}")
                }
            }
            .send()
    }


    //OLD Listen Code
    /*fun startListening(//function for updating the values
        //context: Context,
        temperatureState: MutableState<String>,
        humidityState: MutableState<String>,
        particleState: MutableState<String>,
        jsonState: MutableState<String>
    ) {
        //val dao = DatabaseProvider.getDatabase(context).sensorDataDao()
       // val scope = CoroutineScope(Dispatchers.IO)
        client.subscribeWith()
            .topicFilter("sensor/#")
            .callback { publish ->
                val topic = publish.topic.toString()
                val message = publish.payload
                    .map { buffer ->
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        String(bytes)
                    }.orElse("No payload")
                val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                val messageWithTime = "$message Received at $currentTime"//debugging
                println("MQTT $message Received at $currentTime")
                when (topic) {
                    "sensor/temperature" -> temperatureState.value = message
                    "sensor/humidity" -> humidityState.value = message
                    "sensor/particle" -> particleState.value = message
                    "sensor/json" -> jsonState.value = message
                }
                // Insert into DB only when all values are available.  Trying to make database update whenever topic is read
                /*
                if (
                    temperatureState.value.isNotBlank() &&
                    humidityState.value.isNotBlank() &&
                    particleState.value.isNotBlank()
                ) {
                    val sensorData = SensorData(
                        timeStamp = currentTime,
                        temperatureData = temperatureState.value.toIntOrNull(),
                        humidityData = humidityState.value.toIntOrNull(),
                        particleData = particleState.value.toIntOrNull()
                    )

                    scope.launch {
                        dao.insert(sensorData)
                        Log.d("MQTT", "Sensor data saved to database: $sensorData")
                        }
                    }*/

            }
            .send()
    }*/
}
