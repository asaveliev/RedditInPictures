/*
 * Copyright (C) 2014 Antew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antew.redditinpictures.preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import com.antew.redditinpictures.library.preferences.RedditInPicturesPreferences;
import com.antew.redditinpictures.library.util.AndroidUtil;
import com.antew.redditinpictures.library.util.Ln;
import com.antew.redditinpictures.pro.R;
import com.antew.redditinpictures.util.ConstsFree;

public class RedditInPicturesFreePreferences extends RedditInPicturesPreferences {
    CheckBoxPreference adsPreference;

    /**
     * This uses the deprecated addPreferencesFromResource because fragment preferences aren't part
     * of the support library
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_free);
        adsPreference = (CheckBoxPreference) getPreferenceScreen().findPreference(getString(R.string.pref_disable_ads));

        getPreferenceScreen().findPreference(getString(R.string.pref_get_pro_version))
                             .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                                 @Override
                                 public boolean onPreferenceClick(Preference preference) {
                                     Intent intent = new Intent(Intent.ACTION_VIEW);
                                     intent.setData(Uri.parse(ConstsFree.MARKET_INTENT + ConstsFree.PRO_VERSION_PACKAGE));
                                     startActivity(intent);
                                     return true;
                                 }
                             });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);
        String disableAdsKey = getString(R.string.pref_disable_ads);
        if (key.equals(disableAdsKey)) {
            if (sharedPreferences.getBoolean(key, false) == true && !isFinishing()) {
                new AlertDialog.Builder(this).setTitle(R.string.disable_ads)
                                             .setMessage(R.string.in_exchange_for_disabling_ads_dialog)
                                             .setPositiveButton(R.string.i_ve_rated_it, new DialogInterface.OnClickListener() {
                                                 public void onClick(DialogInterface dialog, int whichButton) {

                                                 }
                                             })
                                             .setNegativeButton(R.string.go_to_market, new DialogInterface.OnClickListener() {
                                                 public void onClick(DialogInterface dialog, int whichButton) {
                                                     if (!AndroidUtil.isUserAMonkey()) {
                                                         Intent intent = new Intent(Intent.ACTION_VIEW);
                                                         intent.setData(Uri.parse(ConstsFree.MARKET_INTENT + getPackageName()));
                                                         startActivity(intent);
                                                     }
                                                 }
                                             })
                                             .show();
            }
            Ln.d("Disable Ads Changed to %s", sharedPreferences.getBoolean(disableAdsKey, false));
        }
    }
}
