package com.pansky.megvill;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.toast.ToastModule;
import com.facebook.react.uimanager.ViewManager;
import com.megvii.livenesslib.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Adi（adi@imeth.cn） on 16/8/18.
 */

public class MegliveReactPackage implements ReactPackage {

    public static int RES_MODEL = R.raw.model;

    public MegliveReactPackage() {
    }

    public MegliveReactPackage(int resModel) {
        RES_MODEL = resModel;
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();

        modules.add(new MegliveModule(reactContext));

        return modules;
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }

}
