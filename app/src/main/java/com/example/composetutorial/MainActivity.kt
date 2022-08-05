package com.example.composetutorial

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import android.os.Handler
import android.os.Looper
import android.content.BroadcastReceiver
import android.content.IntentFilter
import java.util.Collections.addAll

// https://www.codeplayon.com/2022/03/bluetooth-connected-list-in-jetpack/

class MainActivity : ComponentActivity() {
    private var counter = mutableStateOf(0)
    private var selected = mutableStateOf("Male")

    private lateinit var mBluetoothManager:  BluetoothManager
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var mPairedDevices: Set<BluetoothDevice> = emptySet()
    private val mDiscoveredDevicesMutable: MutableState<MutableList<BluetoothDevice>> = mutableStateOf(mutableListOf())

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
        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @SuppressLint("MissingPermission")
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        val list = mutableListOf<BluetoothDevice>()
                        list.apply {addAll(mDiscoveredDevicesMutable.value)}
                        list.add(device)
                        mDiscoveredDevicesMutable.value = list

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

    @Composable
    fun ButtonRootView() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            SimpleButton()
            SimpleList()
            Column {
                SimpleRadioGroup("Male")
                SimpleRadioGroup("Female")
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

    @Composable
    fun SimpleList() {

        LazyColumn {
            item {
                Text(text = "test")
            }
            item {
                Text(text = "test pushed: " + counter.value.toString())
                Text(text = "test: " + mDiscoveredDevicesMutable.value.count().toString())
            }
        }
    }

    @Composable
    fun SimpleRadioGroup(sex: String) {
        Row {
            RadioButton(selected = selected.value == sex, onClick = { selected.value = sex })
            Text(
                    text = sex,
                    modifier = Modifier.clickable(onClick = { selected.value = sex })
            )
        }
    }

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun ButtonPreview() {
        ButtonRootView()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        if (mBluetoothAdapter.isDiscovering)
            mBluetoothAdapter.cancelDiscovery()

        unregisterReceiver(receiver)
    }
}










