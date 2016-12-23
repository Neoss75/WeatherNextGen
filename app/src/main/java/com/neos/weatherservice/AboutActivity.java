package com.neos.weatherservice;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            String LOG_TAG = "WeaServ";
            Log.d(LOG_TAG, e.toString());
        }
        assert pInfo != null;
        String versionName = pInfo.versionName;
        String Text = ("v " + versionName);
        String textAbout = getString(R.string.text_about);
        TextView textView = (TextView) findViewById(R.id.textViewVer);
        TextView aboutView = (TextView) findViewById(R.id.textViewStr);
        textView.setText(Text);
        aboutView.setText(textAbout);

    }

    public void onClickForum(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://4pda.ru/forum/index.php?showtopic=716683"));
        startActivity(intent);
    }

}
