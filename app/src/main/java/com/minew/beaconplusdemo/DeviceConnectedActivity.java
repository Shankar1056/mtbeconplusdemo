package com.minew.beaconplusdemo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.minew.beaconplus.sdk.MTCentralManager;
import com.minew.beaconplus.sdk.MTConnectionHandler;
import com.minew.beaconplus.sdk.MTPeripheral;
import com.minew.beaconplus.sdk.enums.FrameType;
import com.minew.beaconplus.sdk.enums.TriggerType;
import com.minew.beaconplus.sdk.enums.Version;
import com.minew.beaconplus.sdk.exception.MTException;
import com.minew.beaconplus.sdk.frames.MinewFrame;
import com.minew.beaconplus.sdk.interfaces.MTCOperationCallback;
import com.minew.beaconplus.sdk.interfaces.SetTriggerListener;
import com.minew.beaconplus.sdk.model.Trigger;
import com.minew.beaconplusdemo.databinding.ActivityDeviceConnectedCompleteBinding;

import java.util.List;

public class DeviceConnectedActivity extends AppCompatActivity {
    public static final String TAG="Connected";

    private ActivityDeviceConnectedCompleteBinding binding;
    private MTCentralManager mMtCentralManager;
    private MTConnectionHandler mMTConnectionHandler;
    private MTPeripheral mConnectedMTPeripheral;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceConnectedCompleteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initToolBar();
        initListener();
        initData();
    }

    private void initToolBar(){
        setSupportActionBar(binding.toolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void initData(){
        mConnectedMTPeripheral = Config.mConnectedMTPeripheral;
        mMTConnectionHandler = mConnectedMTPeripheral.mMTConnectionHandler;
    }

    private void initListener(){
        binding.saveBroadcastParamsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveBroadcastParams();
            }
        });

        binding.saveTriggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveTrigger();
            }
        });
    }


    public void saveBroadcastParams(){
        List<FrameType> supportedSlots = mConnectedMTPeripheral.mMTConnectionHandler.mTConnectionFeature.getSupportedSlots();
        MinewFrame minewFrame = mConnectedMTPeripheral.mMTConnectionHandler.allFrames.get(0);
        FrameType frameType = minewFrame.getFrameType();
        if (!supportedSlots.contains(frameType)) {
            return;
        }
        int curSlot = minewFrame.getCurSlot();
        minewFrame.setAdvInterval(1000);
        minewFrame.setAdvtxPower(4);
        minewFrame.setRadiotxPower(-4);
        mConnectedMTPeripheral.mMTConnectionHandler.writeSlotFrame(minewFrame, curSlot,new MTCOperationCallback() {
            @Override
            public void onOperation(boolean b, MTException e) {
                Log.e("minew_tag","writeSlotFrame success " + b);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(b){
                            Toast.makeText(DeviceConnectedActivity.this,"writeSlotFrame success",Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(DeviceConnectedActivity.this,"writeSlotFrame fail",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

    }

    public void saveTrigger() {
        int mCurSlot = 2;//需要配置通道的值，2 代表第三通道
        Version version = this.mMTConnectionHandler.mTConnectionFeature.getVersion();
        if (version.getValue() >= 4) {
            if (this.mMTConnectionHandler.mTConnectionFeature.supportTriggers.size() > 0
                    && this.mMTConnectionHandler.triggers.size() > 0) {
                Trigger trigger = new Trigger();
                trigger.setCurSlot(mCurSlot);//选择设置那个通道
                boolean isOpen = true; //代表是否开启了触发器
                if (isOpen) {
                    TriggerType triggerType = TriggerType.BTN_DTAP_EVT;
                    trigger.setTriggerType(TriggerType.BTN_DTAP_EVT);//双击按键
                    switch (triggerType) {
                        case TEMPERATURE_ABOVE_ALARM://温度高于
                        case TEMPERATURE_BELOW_ALARM://温度低于
                        case HUMIDITY_ABOVE_ALRM://湿度高于
                        case HUMIDITY_BELOW_ALRM://湿度低于
                        case LIGHT_ABOVE_ALRM://光感高于
                        case LIGHT_BELOW_ALARM://光感低于
                        case FORCE_ABOVE_ALRM://压感大于
                        case FORCE_BELOW_ALRM://压感低于
                        case TVOC_ABOVE_ALARM://TVOC大于
                        case TVOC_BELOW_ALARM://TVOC低于
                            trigger.setCondition(10);//这些触发条件，时长 mTemCondition 不需要乘 1000
                            break;
                        default:
                            trigger.setCondition(10 * 1000);
                    }
                } else {
                    trigger.setTriggerType(TriggerType.TRIGGER_SRC_NONE);
                    trigger.setCondition(0);
                }
                if (version.getValue() > 4) {
                    trigger.setAdvInterval(2000);//广播间隔 100 ms ~ 5000 ms
                    trigger.setRadioTxpower(0);//广播功率：-40dBm ~ 4dBm
                    trigger.setAlwaysAdvertising(false);//true：总是广播，false：不总是广播
                }
                this.mMTConnectionHandler.setTriggerCondition(trigger, new SetTriggerListener() {
                    @Override
                    public void onSetTrigger(boolean success, MTException mtException) {
                        //Monitor whether the write is successful
                        Log.e("minew_tag","trigger success " + success);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(success){
                                    Toast.makeText(DeviceConnectedActivity.this,"trigger success",Toast.LENGTH_SHORT).show();
                                }else{
                                    Toast.makeText(DeviceConnectedActivity.this,"trigger fail",Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MTCentralManager.getInstance(this).disconnect(mConnectedMTPeripheral);
    }
}
