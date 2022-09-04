package com.example.composetutorial

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import java.util.*


// https://www.codeplayon.com/2022/03/bluetooth-connected-list-in-jetpack/
// https://www.geeksforgeeks.org/radiobuttons-in-android-using-jetpack-compose/

class MainActivity : ComponentActivity() {
    private var temperature = mutableStateOf(0.0f)
    private var humidity = mutableStateOf(0)

    private lateinit var mBluetoothManager:  BluetoothManager
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private val mDiscoveredDevicesMutable: MutableState<MutableList<BluetoothDevice>> = mutableStateOf(mutableListOf())
    private var mBluetoothGatt: BluetoothGatt? = null;

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.i("Bluetooth", ":request permission result ok")
        } else {
            Log.i("Bluetooth", ":request permission result canceled / denied")
        }
    }

    private fun requestBluetoothPermission() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activityResultLauncher.launch(enableBluetoothIntent)
    }

    private val PERMISSIONS_BLUETOOTH = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_PRIVILEGED,
        Manifest.permission.ACCESS_COARSE_LOCATION, // TODO: remove it?
        Manifest.permission.ACCESS_FINE_LOCATION // TODO: remove it?
    )

    private fun checkPermission(type: String)
    {
        val permission =
            ActivityCompat.checkSelfPermission(this, type)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_BLUETOOTH,
                1
            )
        }
    }

    private fun checkPermissions() {
        checkPermission(Manifest.permission.BLUETOOTH)
        checkPermission(Manifest.permission.BLUETOOTH_ADMIN)
        checkPermission(Manifest.permission.BLUETOOTH_SCAN)
        checkPermission(Manifest.permission.BLUETOOTH_CONNECT)
        checkPermission(Manifest.permission.BLUETOOTH_PRIVILEGED)
        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) // TODO: remove?
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) // TODO: remove?
    }

    @SuppressLint("MissingPermission")
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {

                        if (!mDiscoveredDevicesMutable.value.contains(device)) {
                            val list = mutableListOf<BluetoothDevice>()
                            list.apply { addAll(mDiscoveredDevicesMutable.value) }
                            list.add(device)
                            mDiscoveredDevicesMutable.value = list
                        }

                        if (device.name != null) {
                            Log.i(
                                "Bluetooth",
                                "onReceive: device found = " + device.name.toString()
                            )
                        }
                    }
                    Log.i("Bluetooth", "onReceive: Device found")
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.i("Bluetooth", "onReceive: Started Discovery")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i("Bluetooth", "onReceive: Finished Discovery")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun scan() {
        if (mBluetoothAdapter.isDiscovering) {
            mBluetoothAdapter.cancelDiscovery()
            mBluetoothAdapter.startDiscovery()
        } else {
            mBluetoothAdapter.startDiscovery()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            mBluetoothAdapter.cancelDiscovery()
        }, 10000L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val foundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val startFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        val endFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        val nextFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(receiver, foundFilter)
        registerReceiver(receiver, startFilter)
        registerReceiver(receiver, endFilter)
        registerReceiver(receiver, nextFilter)

        checkPermissions()

        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = mBluetoothManager.adapter


        if (!mBluetoothAdapter.isEnabled) {
            requestBluetoothPermission()
        }

        setContent {
            ButtonRootView()
        }
    }

    private fun temperatureColorFromValue(value: Float): Color
    {
        if (value == 25f)
            return Color(0, 255, 0)

        if (value <= 0f)
            return Color(0, 0, 255)

        if (value >= 50f)
            return Color(255, 0, 0)

        if (value < 25f) {
            return Color(0,
                         (255f * value / 25f).toInt(),
                         (255f * (25f - value) / 25f).toInt())
        } else {
            return Color((255f * (value) / 50f).toInt(),
                          (255f * (50f - value) / 50f).toInt(),
                          0)
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun ButtonRootView() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            SimpleButton()

            if (mDiscoveredDevicesMutable.value.size != 0) {
                val bluetoothDevice = SimpleRadioGroup()
                ButtonBluetoothPair(bluetoothDevice)
                ButtonBluetoothDisconnect()
            }

            ComposeCircularTemperatureBar(
                value = temperature.value,
                minValue = -50f,
                maxValue = 50f,
                fillColor = temperatureColorFromValue(temperature.value),
                backgroundColor = Color.Gray,
                strokeWidth = 10.dp
            )

            ComposeCircularHumidityBar(
                value = humidity.value,
                minValue = 0,
                maxValue = 100,
                fillColor = Color.Blue,
                backgroundColor = Color.Gray,
                strokeWidth = 10.dp
            )
        }
    }

    @Composable
    fun SimpleButton() {
        Button(onClick = {
            scan()
        }) {
            Text(text = "Settings")
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun ButtonBluetoothPair(bluetoothDevice: BluetoothDevice) {
        Button(onClick = {
            mBluetoothGatt = bluetoothDevice.connectGatt(this, true, gattCallback)
        }) {
            Text(text = "Pair with selected")
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun ButtonBluetoothDisconnect() {
        Button(onClick = {
            mBluetoothGatt?.disconnect()
        }) {
            Text(text = "Disconnect")
        }
    }


    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            gatt: BluetoothGatt, status: Int,
            newState: Int
        ) {
            Log.d("bluetooth", "newState = $newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("bluetooth", "Connected to GATT server.")
                Log.i(
                    "bluetooth", "Attempting to start service discovery:" +
                            mBluetoothGatt?.discoverServices()
                )
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("bluetooth", "Disconnected from GATT server.")
                //TODO here?
            }
        }

        // New services discovered
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.w("bluetooth", "onServicesDiscovered received: $status")
            var services = gatt.services
            for (service in services) {
                Log.w("bluetooth", service.uuid.toString())
            }
            val service = gatt.getService(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))
            if (service != null) {
                val characteristic = service.characteristics[0]
                if (characteristic != null) gatt.setCharacteristicNotification(
                    characteristic,
                    true
                ) else Log.w("bluetooth", "characteristic is null")
            }
        }

        // Characteristic notification
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val character = characteristic.getStringValue(0) ?: return
            Log.w("bluetooth", "read character: $character")
            val array = character.split(";").toTypedArray()
            if (array.size == 2) {
                temperature.value = array[0].toFloat()
                humidity.value = array[1].toInt()
            }
        }

        // Result of a characteristic read operation
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w("bluetooth", "onCharacteristicRead ")
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun SimpleRadioGroup(): BluetoothDevice {
        var selected by remember { mutableStateOf(mDiscoveredDevicesMutable.value[0]) }

        mDiscoveredDevicesMutable.value.forEach { it ->
            Row(
                Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            )
            {
                if (it.name != null) {
                    RadioButton(selected = selected == it,
                        onClick = { selected = it })
                    Text(
                        text = it.name
                    )
                }
            }
        }
        return selected
    }

    @Composable
    fun ComposeCircularTemperatureBar(
        value: Float,
        minValue: Float,
        maxValue: Float,
        modifier: Modifier = Modifier,
        fillColor: Color,
        backgroundColor: Color,
        strokeWidth: Dp
    ) {
        val paint = Paint().apply {
            textAlign = Paint.Align.CENTER
            textSize = 50f
            color = Color.Black.toArgb()
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        Canvas(
            modifier = modifier
                .size(150.dp)
                .padding(10.dp),
        ) {
            // Background Line
            drawArc(
                color = backgroundColor,
                140f,
                260f,
                false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )

            if (value < minValue || value > maxValue)
                return@Canvas

            // Fill Line
            drawArc(
                color = fillColor,
                140f,
                260f * ((maxValue - minValue) / 2 + value) / (maxValue - minValue),
                false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )

            drawContext.canvas.nativeCanvas.drawText("$value°C", center.x, center.y, paint)
        }
    }

    @Composable
    fun ComposeCircularHumidityBar(
        value: Int,
        minValue: Int,
        maxValue: Int,
        modifier: Modifier = Modifier,
        fillColor: Color,
        backgroundColor: Color,
        strokeWidth: Dp
    ) {
        val paint = Paint().apply {
            textAlign = Paint.Align.CENTER
            textSize = 50f
            color = Color.Black.toArgb()
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        Canvas(
            modifier = modifier
                .size(150.dp)
                .padding(10.dp)
        ) {
            // Background Line
            drawArc(
                color = backgroundColor,
                140f,
                260f,
                false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )

            if (value < minValue || value > maxValue)
                return@Canvas

            // Fill Line
            drawArc(
                color = fillColor,
                140f,
                260f * value / maxValue,
                false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )

            drawContext.canvas.nativeCanvas.drawText("$value%", center.x, center.y, paint)
        }
    }


    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun ButtonPreview() {
        ButtonRootView()
    }

    @SuppressLint("MissingPermission") // TODO: why supress?
    override fun onDestroy() { // TODO: all other to destroy
        super.onDestroy()
        if (mBluetoothAdapter.isDiscovering) {
            mBluetoothAdapter.cancelDiscovery()
        }

        unregisterReceiver(receiver)
    }
}










