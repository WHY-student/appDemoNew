package com.gdu.demo.widgetlist.lighttype

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.gdu.camera.SettingsDefinitions
import com.gdu.demo.R
import com.gdu.demo.SdkDemoApplication
import com.gdu.demo.databinding.LayoutLightSelectedBinding
import com.gdu.demo.ourgdu.ourGDUAircraft
import com.gdu.sdk.camera.GDUCamera
import com.gdu.sdk.gimbal.GDUGimbal
import com.gdu.ux.core.base.widget.ConstraintLayoutWidget
import com.gdu.demo.FlightActivity;


class LightSelectedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<LightTypeModel>(context, attrs, defStyleAttr) {

    private lateinit var binding: LayoutLightSelectedBinding
    private var mGDUGimbal: GDUGimbal? = null
    private var currentType = -1
    private var buttonsEnabled = true
    private var action: Runnable? = null

    interface OnButtonStateChangeListener {
        fun onButtonStateChanged(enabled: Boolean)
    }
    private var buttonStateListener: OnButtonStateChangeListener? = null

    interface OnModeSelectedListener {
        fun onModeSelected(mode: SettingsDefinitions.DisplayMode)
    }

    private var modeSelectedListener: OnModeSelectedListener? = null

    // 设置监听器方法
    fun setOnModeSelectedListener(listener: OnModeSelectedListener) {
        this.modeSelectedListener = listener
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        binding = LayoutLightSelectedBinding.bind(inflate(context, R.layout.layout_light_selected, this))

//        setupButtonClickListeners()
        initGimbalSupportModes()
    }

    public fun setupSelectButtonClick(action: Runnable){
        this.action = action
        binding.tvVisibleLight.setOnClickListener {
            if (buttonsEnabled) changeLight(SettingsDefinitions.DisplayMode.VISUAL_ONLY)
        }
        binding.tvWide.setOnClickListener {
            if (buttonsEnabled) changeLight(SettingsDefinitions.DisplayMode.WAL)
        }
        binding.tvZoom.setOnClickListener {
            if (buttonsEnabled) changeLight(SettingsDefinitions.DisplayMode.ZL)
        }
        binding.tvIr.setOnClickListener {
            if (buttonsEnabled) changeLight(SettingsDefinitions.DisplayMode.THERMAL_ONLY)
        }
        binding.tvSplit.setOnClickListener {
            if (buttonsEnabled) changeLight(SettingsDefinitions.DisplayMode.PIP)
        }
    }

//    private fun setupButtonClickListeners() {
//        binding.tvVisibleLight.setOnClickListener {
//            if (buttonsEnabled) changeLight(SettingsDefinitions.DisplayMode.VISUAL_ONLY)
//        }
//        binding.tvWide.setOnClickListener {
//            if (buttonsEnabled) changeLight(SettingsDefinitions.DisplayMode.WAL)
//        }
//        binding.tvZoom.setOnClickListener {
//            if (buttonsEnabled) changeLight(SettingsDefinitions.DisplayMode.ZL)
//        }
//        binding.tvIr.setOnClickListener {
//            if (buttonsEnabled) changeLight(SettingsDefinitions.DisplayMode.THERMAL_ONLY)
//        }
//        binding.tvSplit.setOnClickListener {
//            if (buttonsEnabled) changeLight(SettingsDefinitions.DisplayMode.PIP)
//        }
//    }

    private fun initGimbalSupportModes() {
        mGDUGimbal = (SdkDemoApplication.getProductInstance() as ourGDUAircraft).gimbal as? GDUGimbal
        mGDUGimbal?.let { gimbal ->
            gimbal.supportDisplayMode.forEach { mode ->
                when (mode) {
                    SettingsDefinitions.DisplayMode.THERMAL_ONLY -> binding.tvIr.visibility = VISIBLE
                    SettingsDefinitions.DisplayMode.VISUAL_ONLY -> binding.tvVisibleLight.visibility = VISIBLE
                    SettingsDefinitions.DisplayMode.WAL -> binding.tvWide.visibility = VISIBLE
                    SettingsDefinitions.DisplayMode.ZL -> binding.tvZoom.visibility = VISIBLE
                    SettingsDefinitions.DisplayMode.PIP -> binding.tvSplit.visibility = VISIBLE
                    else -> {}
                }
            }
        }
    }

    override fun initWidgetModel(): LightTypeModel = LightTypeModel()

    override fun bindingData(data: Any) {
        when (data) {
            is LightTypeValue -> handleLightTypeValue(data)
            is Boolean -> setButtonsEnabled(data)
        }
    }

    private fun handleLightTypeValue(data: LightTypeValue) {
        if (data.lightType == currentType) return
        currentType = data.lightType
        mGDUGimbal = (SdkDemoApplication.getProductInstance() as ourGDUAircraft).gimbal as? GDUGimbal
        mGDUGimbal?.let { gimbal ->
            gimbal.supportDisplayMode.forEach { mode ->
                updateButtonVisibility(mode, data.lightType)
            }
        }
        this.action?.run()
    }

    private fun updateButtonVisibility(mode: SettingsDefinitions.DisplayMode, lightType: Int) {
        val visible = when (lightType) {
            0x00 -> mode != SettingsDefinitions.DisplayMode.THERMAL_ONLY
            0x02 -> mode != SettingsDefinitions.DisplayMode.VISUAL_ONLY
            0x05 -> mode != SettingsDefinitions.DisplayMode.WAL
            0x06 -> mode != SettingsDefinitions.DisplayMode.ZL
            0x07 -> mode != SettingsDefinitions.DisplayMode.PIP
            else -> true
        }

        when (mode) {
            SettingsDefinitions.DisplayMode.THERMAL_ONLY -> binding.tvIr.visibility = if (visible) VISIBLE else GONE
            SettingsDefinitions.DisplayMode.VISUAL_ONLY -> binding.tvVisibleLight.visibility = if (visible) VISIBLE else GONE
            SettingsDefinitions.DisplayMode.WAL -> binding.tvWide.visibility = if (visible) VISIBLE else GONE
            SettingsDefinitions.DisplayMode.ZL -> binding.tvZoom.visibility = if (visible) VISIBLE else GONE
            SettingsDefinitions.DisplayMode.PIP -> binding.tvSplit.visibility = if (visible) VISIBLE else GONE
            else -> {}
        }
    }

    fun setButtonsEnabled(enabled: Boolean) {
        buttonsEnabled = enabled
        updateButtonsState()
        buttonStateListener?.onButtonStateChanged(enabled)
    }

    fun setOnButtonStateChangeListener(listener: OnButtonStateChangeListener) {
        this.buttonStateListener = listener
    }

    private fun updateButtonsState() {
        val alpha = if (buttonsEnabled) 1.0f else 0.5f
        binding.tvVisibleLight.isEnabled = buttonsEnabled
        binding.tvWide.isEnabled = buttonsEnabled
        binding.tvZoom.isEnabled = buttonsEnabled
        binding.tvIr.isEnabled = buttonsEnabled
        binding.tvSplit.isEnabled = buttonsEnabled

        binding.tvVisibleLight.alpha = alpha
        binding.tvWide.alpha = alpha
        binding.tvZoom.alpha = alpha
        binding.tvIr.alpha = alpha
        binding.tvSplit.alpha = alpha
    }

    private fun changeLight(type: SettingsDefinitions.DisplayMode) {
        if (!buttonsEnabled) {
            Toast.makeText(context, "当前模式不可操作", Toast.LENGTH_SHORT).show()
            return
        }

        val mGDUCamera = (SdkDemoApplication.getProductInstance() as ourGDUAircraft).camera as? GDUCamera
        mGDUCamera?.setDisplayMode(type) { error ->
            val message = if (error == null) "设置成功" else "设置失败"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    fun setButtonTextColor(colorRes: Int) {
        val color = ContextCompat.getColor(context, colorRes)
        binding.tvVisibleLight.setTextColor(color)
        binding.tvWide.setTextColor(color)
        binding.tvZoom.setTextColor(color)
        binding.tvIr.setTextColor(color)
        binding.tvSplit.setTextColor(color)
    }

    fun setButtonBackgroundColor(colorRes: Int) {
        val color = ContextCompat.getColor(context, colorRes)
        binding.tvVisibleLight.setBackgroundColor(color)
        binding.tvWide.setBackgroundColor(color)
        binding.tvZoom.setBackgroundColor(color)
        binding.tvIr.setBackgroundColor(color)
        binding.tvSplit.setBackgroundColor(color)
    }
    public fun get_irselected(): Int{
        Log.d("get_irselected: ", ""+currentType)
        if (currentType==0x00 ){
            return 1
        }else if(currentType==0x07 ){
            return 0
        }else{
            return 2
        }
    }

}