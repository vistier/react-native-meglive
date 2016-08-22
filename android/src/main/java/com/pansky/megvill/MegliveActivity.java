package com.pansky.megvill;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.megvii.livenesslib.LivenessActivity;

/**
 * Created by Adi（adi@imeth.cn） on 16/8/18.
 */
public class MegliveActivity extends Activity {

    private static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivityForResult(new Intent(this, LivenessActivity.class), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            // 回调数据给module,
            MegliveModule.instance.onActivityResult(resultCode, data);

            finish();
        }
    }
}
