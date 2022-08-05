package com.example.composetutorial

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import java.util.*


// https://www.codeplayon.com/2022/03/bluetooth-connected-list-in-jetpack/
// https://www.geeksforgeeks.org/radiobuttons-in-android-using-jetpack-compose/

class MainActivity : ComponentActivity() {
    private var counter = mutableStateOf(0)
    private var temperature = mutableStateOf(0.0)
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
            SimpleList()

            if (mDiscoveredDevicesMutable.value.size != 0) {
                val bluetoothDevice = SimpleRadioGroup()
                ButtonBluetoothPair(bluetoothDevice)
                ButtonBluetoothDisconnect()
            }
        }
    }

    @Composable
    fun SimpleButton() {
        Button(onClick = {
            scan()
            counter.value = counter.value + 1
        }) {
            Text(text = "Simple Button")
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
                temperature.value = array[0].toDouble()
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

    @Composable
    fun SimpleList() {
        LazyColumn {
            item {
                Text(text = "test")
            }
            item {
                Text(text = "Temperature: " + temperature.value.toString())
                Text(text = "Humidity: " + humidity.value.toString())
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

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun ButtonPreview() {
        ButtonRootView()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        if (mBluetoothAdapter.isDiscovering) {
            mBluetoothAdapter.cancelDiscovery()
        }

        unregisterReceiver(receiver)
    }
}










