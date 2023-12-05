package com.minew.beaconplusdemo;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.kongzue.dialogx.dialogs.WaitDialog;

import com.minew.beaconplus.sdk.MTCentralManager;
import com.minew.beaconplus.sdk.MTPeripheral;
import com.minew.beaconplus.sdk.Utils.BLETool;
import com.minew.beaconplus.sdk.enums.ConnectionStatus;
import com.minew.beaconplus.sdk.exception.MTException;
import com.minew.beaconplus.sdk.interfaces.ConnectionStatueListener;
import com.minew.beaconplus.sdk.interfaces.GetPasswordListener;
import com.minew.beaconplus.sdk.interfaces.MTCentralManagerListener;
import com.minew.beaconplusdemo.databinding.ActivityMainBinding;
import com.permissionx.guolindev.PermissionX;
import com.permissionx.guolindev.callback.ExplainReasonCallback;
import com.permissionx.guolindev.callback.ForwardToSettingsCallback;
import com.permissionx.guolindev.callback.RequestCallback;
import com.permissionx.guolindev.request.ExplainScope;
import com.permissionx.guolindev.request.ForwardScope;

import java.util.List;

public class ScanDeviceListActivity extends AppCompatActivity {

    public static final String TAG="Scan";
    private ActivityMainBinding binding;

    private ScanDevicesListAdapter mDevicesListAdapter;
    private ObjectAnimator mObjectAnimator;

    private MTCentralManager mMtCentralManager;

    private MTPeripheral mConnectedMTPeripheral;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initToolBar();
        initRefresh();
        initRecyclerView();
        initAnimator();
        initBleManager();
        setBleManagerListener();
        initBlePermission();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initToolBar(){
        setSupportActionBar(binding.toolBar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }


    private void initRefresh(){
        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                stopScan();
                startScan();
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void initRecyclerView(){
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(binding.recyclerView.getContext()));
        mDevicesListAdapter = new ScanDevicesListAdapter(R.layout.item_scan_list,null);

        binding.recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayout.VERTICAL));
        mDevicesListAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
                mConnectedMTPeripheral =  mDevicesListAdapter.getItem(position);
                stopScan();
                connectedDevice(mConnectedMTPeripheral);
            }
        });

        binding.recyclerView.setAdapter(mDevicesListAdapter);
    }

    private void initAnimator(){
        mObjectAnimator = ObjectAnimator.ofFloat(binding.ibHomeScan, "rotation", 0f, 360f);
        mObjectAnimator.setDuration(1500);
        //无限循环
        mObjectAnimator.setRepeatCount(ValueAnimator.INFINITE);
    }

    private void initBleManager(){
        mMtCentralManager = MTCentralManager.getInstance(this);
        mMtCentralManager.startService();
    }
    private void setBleManagerListener(){
        mMtCentralManager.setMTCentralManagerListener(new MTCentralManagerListener() {
            @Override
            public void onScanedPeripheral(List<MTPeripheral> list) {
                Log.d(TAG,"onScanedPeripheral list.size="+list.size());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDevicesListAdapter.setList(list);
                    }
                });
            }
        });
    }

    private void initBlePermission(){
        String[] requestPermissionList;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionList = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION};
        } else {
            requestPermissionList = new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION};
        }
        PermissionX.init(this).permissions(requestPermissionList)
                .onExplainRequestReason(new ExplainReasonCallback() {
                    @Override
                    public void onExplainReason(@NonNull ExplainScope scope, @NonNull List<String> deniedList) {
                        scope.showRequestReasonDialog(deniedList,getString(R.string.need_permission_continue),"Ok","Cancel");
                    }
                })
                .onForwardToSettings(new ForwardToSettingsCallback() {
                    @Override
                    public void onForwardToSettings(@NonNull ForwardScope scope, @NonNull List<String> deniedList) {
                        scope.showForwardToSettingsDialog(deniedList,getString(R.string.allow_permission_in_settings),"Ok","Cancel");
                    }
                })
                .request(new RequestCallback() {
                    @Override
                    public void onResult(boolean allGranted, @NonNull List<String> grantedList, @NonNull List<String> deniedList) {
                        if(allGranted){
                            if(BLETool.isBluetoothTurnOn(ScanDeviceListActivity.this)){
                                startScan();
                            }else{
                                BLETool.setBluetoothTurnOn(ScanDeviceListActivity.this);
                            }
                        }
                    }
                });
    }


    private void startScan(){
        Log.d(TAG,"startScan");
        mDevicesListAdapter.setList(null);
        mMtCentralManager.startScan();
        mObjectAnimator.start();
    }

    private void stopScan(){
        Log.d(TAG,"stopScan");
        mMtCentralManager.stopScan();
        mObjectAnimator.cancel();
    }

    private void connectedDevice(MTPeripheral mtPeripheral){
        WaitDialog.show(R.string.loading);
//        mMtCentralManager.connect(mtPeripheral,connectionStatueListener);
        mMtCentralManager.connect(mtPeripheral,new ConnectionStatueListener() {
            @Override
            public void onUpdateConnectionStatus(final ConnectionStatus connectionStatus, final GetPasswordListener getPasswordListener) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (connectionStatus) {
                            case CONNECTING:
                                Log.e("tag", "CONNECTING");
                                Toast.makeText(ScanDeviceListActivity.this, "CONNECTING", Toast.LENGTH_SHORT).show();
                                break;
                            case CONNECTED:
                                Log.e("tag", "CONNECTED");
                                Toast.makeText(ScanDeviceListActivity.this, "CONNECTED", Toast.LENGTH_SHORT).show();
                                break;
                            case READINGINFO:
                                Log.e("tag", "READINGINFO");
                                Toast.makeText(ScanDeviceListActivity.this, "READINGINFO", Toast.LENGTH_SHORT).show();
                                break;
                            case DEVICEVALIDATING:
                                Log.e("tag", "DEVICEVALIDATING");
                                Toast.makeText(ScanDeviceListActivity.this, "DEVICEVALIDATING", Toast.LENGTH_SHORT).show();
                                break;
                            case PASSWORDVALIDATING:
                                Log.e("tag", "PASSWORDVALIDATING");
                                Toast.makeText(ScanDeviceListActivity.this, "PASSWORDVALIDATING", Toast.LENGTH_SHORT).show();
                                String password = "minew123";
                                getPasswordListener.getPassword(password);
                                break;
                            case SYNCHRONIZINGTIME:
                                Log.e("tag", "SYNCHRONIZINGTIME");
                                Toast.makeText(ScanDeviceListActivity.this, "SYNCHRONIZINGTIME", Toast.LENGTH_SHORT).show();
                                break;
                            case READINGCONNECTABLE:
                                Log.e("tag", "READINGCONNECTABLE");
                                Toast.makeText(ScanDeviceListActivity.this, "READINGCONNECTABLE", Toast.LENGTH_SHORT).show();
                                break;
                            case READINGFEATURE:
                                Log.e("tag", "READINGFEATURE");
                                Toast.makeText(ScanDeviceListActivity.this, "READINGFEATURE", Toast.LENGTH_SHORT).show();
                                break;
                            case READINGFRAMES:
                                Log.e("tag", "READINGFRAMES");
                                Toast.makeText(ScanDeviceListActivity.this, "READINGFRAMES", Toast.LENGTH_SHORT).show();
                                break;
                            case READINGTRIGGERS:
                                Log.e("tag", "READINGTRIGGERS");
                                Toast.makeText(ScanDeviceListActivity.this, "READINGTRIGGERS", Toast.LENGTH_SHORT).show();
                                break;
                            case COMPLETED:
                                WaitDialog.dismiss();
                                Log.e("tag", "COMPLETED");
                                Toast.makeText(ScanDeviceListActivity.this, "COMPLETED", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent();
                                intent.setClass(ScanDeviceListActivity.this, DeviceConnectedActivity.class);
                                startActivity(intent);
                                break;
                            case CONNECTFAILED:
                            case DISCONNECTED:
                                WaitDialog.dismiss();
                                Log.e("tag", "DISCONNECTED");
                                Toast.makeText(ScanDeviceListActivity.this, "DISCONNECTED", Toast.LENGTH_SHORT).show();
                                break;
                        }

                    }
                });
            }

            @Override
            public void onError(MTException e) {
                Log.e("tag", e.getMessage());
            }
        });
        Config.mConnectedMTPeripheral = mtPeripheral;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMtCentralManager.stopService();
        stopScan();
    }
}
