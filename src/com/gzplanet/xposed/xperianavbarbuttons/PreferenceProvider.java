package com.gzplanet.xposed.xperianavbarbuttons;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

public class PreferenceProvider extends RemotePreferenceProvider {
    public PreferenceProvider() {
        super(Utils.PREF_AUTHORITY, new String[]{Utils.PREF_NAME});
    }
}
