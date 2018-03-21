package com.gzplanet.xposed.xperianavbarbuttons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ((intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED) || intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

            Intent intent2 = new Intent();
            intent2.setAction(XperiaNavBarButtons.ACTION_NAVBAR_CHANGED);
            intent2.putExtra("order_list", pref.getString("pref_order", XperiaNavBarButtons.DEF_BUTTONS_ORDER_LIST));
            intent2.putExtra("show_menu", pref.getBoolean("pref_show_menu", true));
            intent2.putExtra("show_toast", true);
            intent2.putExtra("boot", true);
            context.sendBroadcast(intent2);

        }
    }
}
