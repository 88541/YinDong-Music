package com.yindong.music.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 蓝牙耳机管理器
 * 用于检测蓝牙耳机的连接状态和设备信息
 */
class BluetoothHeadsetManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothHeadsetManager"
        private const val HEADSET_CHECK_DELAY = 500L // 延迟检查，确保系统已完成连接
    }

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _headsetConnected = MutableStateFlow(false)
    val headsetConnected: StateFlow<Boolean> = _headsetConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _headsetType = MutableStateFlow(HeadsetType.UNKNOWN)
    val headsetType: StateFlow<HeadsetType> = _headsetType.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())

    enum class HeadsetType {
        WIRED,      // 有线耳机
        BLUETOOTH,  // 蓝牙耳机
        USB,        // USB耳机
        UNKNOWN     // 未知类型
    }

    /**
     * 耳机连接状态数据类
     */
    data class HeadsetState(
        val isConnected: Boolean,
        val deviceName: String?,
        val type: HeadsetType
    )

    private val _headsetState = MutableStateFlow(HeadsetState(false, null, HeadsetType.UNKNOWN))
    val headsetState: StateFlow<HeadsetState> = _headsetState.asStateFlow()

    /**
     * 蓝牙设备广播接收器
     */
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    Log.d(TAG, "蓝牙设备已连接: ${device?.name}")
                    // 延迟检查，等待系统完成音频路由切换
                    handler.postDelayed({ checkHeadsetConnection() }, HEADSET_CHECK_DELAY)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Log.d(TAG, "蓝牙设备已断开")
                    handler.postDelayed({ checkHeadsetConnection() }, HEADSET_CHECK_DELAY)
                }
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
                    Log.d(TAG, "蓝牙连接状态变化: $state")
                    handler.postDelayed({ checkHeadsetConnection() }, HEADSET_CHECK_DELAY)
                }
                AudioManager.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", 0)
                    Log.d(TAG, "有线耳机状态变化: $state")
                    handler.postDelayed({ checkHeadsetConnection() }, HEADSET_CHECK_DELAY)
                }
            }
        }
    }

    /**
     * 初始化并开始监听耳机连接状态
     */
    fun startMonitoring() {
        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
        }
        context.registerReceiver(bluetoothReceiver, filter)

        // 立即检查一次当前状态
        checkHeadsetConnection()
    }

    /**
     * 停止监听
     */
    fun stopMonitoring() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            // 接收器可能未注册
            Log.w(TAG, "Receiver not registered")
        }
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * 检查耳机连接状态
     */
    fun checkHeadsetConnection() {
        val audioDevices = getConnectedAudioDevices()
        val hasHeadset = audioDevices.isNotEmpty()

        // 判断耳机类型
        val type = when {
            audioDevices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                              it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } -> HeadsetType.BLUETOOTH
            audioDevices.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                              it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES } -> HeadsetType.WIRED
            audioDevices.any { it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                              it.type == AudioDeviceInfo.TYPE_USB_DEVICE } -> HeadsetType.USB
            else -> HeadsetType.UNKNOWN
        }

        // 获取设备名称
        val deviceName = when (type) {
            HeadsetType.BLUETOOTH -> getBluetoothDeviceName()
            HeadsetType.WIRED -> "有线耳机"
            HeadsetType.USB -> "USB耳机"
            else -> null
        }

        _headsetConnected.value = hasHeadset
        _connectedDeviceName.value = deviceName
        _headsetType.value = type
        _headsetState.value = HeadsetState(hasHeadset, deviceName, type)

        Log.d(TAG, "耳机状态: connected=$hasHeadset, type=$type, name=$deviceName")
    }

    /**
     * 获取已连接的音频设备列表
     */
    private fun getConnectedAudioDevices(): List<AudioDeviceInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter { device ->
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_USB_DEVICE
                }
        } else {
            emptyList()
        }
    }

    /**
     * 获取已连接的蓝牙设备名称
     */
    private fun getBluetoothDeviceName(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 使用 BluetoothManager
                val connectedDevices = bluetoothManager?.getConnectedDevices(BluetoothProfile.A2DP)
                connectedDevices?.firstOrNull()?.name
            } else {
                // 旧版本使用 BluetoothAdapter
                @Suppress("DEPRECATION")
                val bondedDevices = bluetoothAdapter?.bondedDevices
                bondedDevices?.firstOrNull { device ->
                    try {
                        val method = device.javaClass.getMethod("isConnected")
                        method.invoke(device) as? Boolean == true
                    } catch (e: Exception) {
                        false
                    }
                }?.name
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "缺少蓝牙权限，无法获取设备名称")
            null
        } catch (e: Exception) {
            Log.e(TAG, "获取蓝牙设备名称失败", e)
            null
        }
    }

    /**
     * 检查是否有耳机连接（包括有线、蓝牙、USB）
     */
    fun isHeadsetConnected(): Boolean {
        return _headsetConnected.value
    }

    /**
     * 检查是否连接了蓝牙耳机
     */
    fun isBluetoothHeadsetConnected(): Boolean {
        return _headsetType.value == HeadsetType.BLUETOOTH && _headsetConnected.value
    }

    /**
     * 获取已连接耳机的显示名称
     */
    fun getConnectedHeadsetName(): String? {
        return _connectedDeviceName.value
    }
}
