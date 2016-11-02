package com.selfservit.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class mInterfaceReceiver extends BroadcastReceiver {
    public mInterfaceReceiver() {
		//constructor
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            Intent serviceIntent = new Intent(context, mInterfaceService.class);
            context.startService(serviceIntent);
        }
    }
}
