package com.sensors;

import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.support.annotation.Nullable;
import android.view.WindowManager;
import android.view.Surface;
import android.app.Activity;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class Rotation extends ReactContextBaseJavaModule implements SensorEventListener {

  private final ReactApplicationContext reactContext;
  private final SensorManager sensorManager;
  private final Sensor sensor;
  private double lastReading = (double) System.currentTimeMillis();
  private int interval;
  private Arguments arguments;

  public Rotation(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.sensorManager = (SensorManager)reactContext.getSystemService(reactContext.SENSOR_SERVICE);
    this.sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
  }

  // RN Methods
  @ReactMethod
  public void isAvailable(Promise promise) {
    if (this.sensor == null) {
      // No sensor found, throw error
      promise.reject(new RuntimeException("No Rotation Vector found"));
      return;
    }
    promise.resolve(null);
  }

  @ReactMethod
  public void setUpdateInterval(int newInterval) {
    this.interval = newInterval;
  }

  @ReactMethod
  public void startUpdates() {
    // Milisecond to Mikrosecond conversion
    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
  }

  @ReactMethod
  public void stopUpdates() {
    sensorManager.unregisterListener(this);
  }

  @Override
  public String getName() {
    return "Rotation";
  }

  // SensorEventListener Interface
  private void sendEvent(String eventName, @Nullable WritableMap params) {
    try {
      this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
    } catch (RuntimeException e) {
      Log.e("ERROR", "java.lang.RuntimeException: Trying to invoke Javascript before CatalystInstance has been set!");
    }
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    double tempMs = (double) System.currentTimeMillis();
    if (tempMs - lastReading >= interval){
      lastReading = tempMs;

      Sensor mySensor = sensorEvent.sensor;
      if (mySensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
        float[] rotationMatrix = new float[9];
        float[] orientation = new float[3];

        // Flip Upside-Down
        float[] values = new float[4];
        values[0] = - sensorEvent.values[0];
        values[1] = - sensorEvent.values[1];
        values[2] = sensorEvent.values[2];
        values[3] = sensorEvent.values[3];

        SensorManager.getRotationMatrixFromVector(rotationMatrix, values);
        SensorManager.getOrientation(rotationMatrix, orientation);

        // Flip Tilt and Turn Left-Right
        double alpha = - Math.toDegrees(orientation[0] + Math.PI);
        double beta = Math.toDegrees(orientation[1]);
        double gamma = - Math.toDegrees(orientation[2]);

        int screenOrientation = 0;
        switch (((WindowManager) getCurrentActivity().getSystemService(Activity.WINDOW_SERVICE))
                .getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                screenOrientation = 90;
                break;
            case Surface.ROTATION_180:
                screenOrientation = 180;
                break;
            case Surface.ROTATION_270:
                screenOrientation = -90;
                break;
        }

        WritableMap map = arguments.createMap();

        map.putDouble("alpha", alpha);
        map.putDouble("beta", beta);
        map.putDouble("gamma", gamma);
        map.putDouble("orientation", screenOrientation);

        sendEvent("Rotation", map);
      }
    }
  }
  
  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }
}
