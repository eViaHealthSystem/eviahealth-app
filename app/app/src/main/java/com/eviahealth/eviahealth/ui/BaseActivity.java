package com.eviahealth.eviahealth.ui;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //region :: HIDE SYSTEMBAR AND NAVIGATIONBARS
        Window window = this.getWindow();
        View decorView = this.getWindow().getDecorView();

        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat controllerCompat = new WindowInsetsControllerCompat(window, decorView);
        controllerCompat.hide(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.navigationBars());
        controllerCompat.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        //endregion

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

//        PermissionUtils.requestAll(this);
    }

    @Override
    public void onBackPressed () {}

}
