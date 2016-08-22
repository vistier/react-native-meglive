package com.megvii.livenesslib.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 *传感器工具类 
 */
public class SensorUtil implements SensorEventListener {
	private SensorManager mSensorManager;
	private Sensor mSensor;
	private float Y;

	public SensorUtil(Context context) {
		init(context);
	}

	private void init(Context context) {
		mSensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (mSensor != null) {
			mSensorManager.registerListener(this, mSensor,
					SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Y = event.values[1];
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void release() {
		if (mSensor != null && mSensorManager != null) {
			mSensorManager.unregisterListener(this);
		}
	}

	public boolean isVertical() {
		if (Y >= 7)
			return true;

		return false;
	}
}
