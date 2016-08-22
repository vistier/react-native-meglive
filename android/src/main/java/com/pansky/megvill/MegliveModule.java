package com.pansky.megvill;

import android.app.Activity;
import android.content.Intent;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import static android.R.attr.data;

/**
 * Created by Adi（adi@imeth.cn） on 16/8/18.
 */

public class MegliveModule extends ReactContextBaseJavaModule {

    static MegliveModule instance;

    public MegliveModule(ReactApplicationContext reactContext) {
        super(reactContext);
        instance = this;
    }

    @Override
    public String getName() {
        return "Meglive";
    }

    protected void onActivityResult(int resultCode, Intent data) {
        WritableMap params = Arguments.createMap();

        try {
            if (Activity.RESULT_OK == resultCode && data != null) {
                String result = data.getStringExtra("result");
                JSONTokener parser = new JSONTokener(result);
                params.putString("status", resultCode == Activity.RESULT_OK ? "success" : "failure");
                JSONObject json = (JSONObject) parser.nextValue();
                params.putString("result", json.getString("result"));
            } else {
                params.putString("result", "无效认证");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            params.putString("result", "发生异常");
        }

        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onDetectingResult", params);
    }

    /**
     * 启动认证检测, 通过事件回调返回结果
     */
    @ReactMethod
    public void detecting() {
        getCurrentActivity().startActivity(new Intent(getCurrentActivity(), MegliveActivity.class));
    }
}
