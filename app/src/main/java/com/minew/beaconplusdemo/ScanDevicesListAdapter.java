package com.minew.beaconplusdemo;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.minew.beaconplus.sdk.MTPeripheral;


import java.util.List;

public class ScanDevicesListAdapter extends BaseQuickAdapter<MTPeripheral, BaseViewHolder> {


    public ScanDevicesListAdapter(int layoutResId, List<MTPeripheral> data) {
        super(layoutResId,data);

    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, MTPeripheral mtPeripheral) {


        baseViewHolder.setText(R.id.tv_scan_device_mac,"Mac:"+mtPeripheral.mMTFrameHandler.getMac())
                .setText(R.id.tv_scan_device_battery,"Battery:"+mtPeripheral.mMTFrameHandler.getBattery()+"%")
                .setText(R.id.tv_scan_device_rssi,"Rssi:"+mtPeripheral.mMTFrameHandler.getRssi()+"Dbm");




    }


}
