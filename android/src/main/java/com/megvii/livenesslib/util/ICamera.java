package com.megvii.livenesslib.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.view.Surface;
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 照相机工具类
 */
public class ICamera {

	public Camera mCamera;
	public int cameraWidth;
	public int cameraHeight;
	private int cameraId = 1;//前置摄像头

	public ICamera() {
	}

	/**
	 * 打开相机
	 */
	public Camera openCamera(Activity activity) {
		try {
			mCamera = Camera.open(cameraId);
			CameraInfo cameraInfo = new CameraInfo();
			Camera.getCameraInfo(cameraId, cameraInfo);
			Camera.Parameters params = mCamera.getParameters();
			Camera.Size bestPreviewSize = calBestPreviewSize(
					mCamera.getParameters(), 640, 480);
			cameraWidth = bestPreviewSize.width;
			cameraHeight = bestPreviewSize.height;
			params.setPreviewSize(cameraWidth, cameraHeight);
			mCamera.setDisplayOrientation(getCameraAngle(activity));
			mCamera.setParameters(params);
			return mCamera;
		} catch (Exception e) {
			return null;
		}
	}

	// 通过屏幕参数、相机预览尺寸计算布局参数
	public RelativeLayout.LayoutParams getLayoutParam() {
		Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
		float scale = Math.min(Screen.mWidth * 1.0f / previewSize.height,
				Screen.mHeight * 1.0f / previewSize.width);
		int layout_width = (int) (scale * previewSize.height);
		int layout_height = (int) (scale * previewSize.width);

		RelativeLayout.LayoutParams layout_params = new RelativeLayout.LayoutParams(
				layout_width, layout_height);

		return layout_params;
	}

	/**
	 * 开始检测脸
	 */
	public void actionDetect(Camera.PreviewCallback mActivity) {
		if (mCamera != null) {
			mCamera.setPreviewCallback(mActivity);
		}
	}

	public void startPreview(SurfaceTexture surfaceTexture) {
		if (mCamera != null) {
			try {
				mCamera.setPreviewTexture(surfaceTexture);
				mCamera.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void closeCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	/**
	 * 通过传入的宽高算出最接近于宽高值的相机大小
	 */
	private Camera.Size calBestPreviewSize(Camera.Parameters camPara,
										   final int width, final int height) {
		List<Camera.Size> allSupportedSize = camPara.getSupportedPreviewSizes();
		ArrayList<Camera.Size> widthLargerSize = new ArrayList<Camera.Size>();
		for (Camera.Size tmpSize : allSupportedSize) {
			if (tmpSize.width > tmpSize.height) {
				widthLargerSize.add(tmpSize);
			}
		}

		Collections.sort(widthLargerSize, new Comparator<Camera.Size>() {
			@Override
			public int compare(Camera.Size lhs, Camera.Size rhs) {
				int off_one = Math.abs(lhs.width * lhs.height - width * height);
				int off_two = Math.abs(rhs.width * rhs.height - width * height);
				return off_one - off_two;
			}
		});

		return widthLargerSize.get(0);
	}

	/**
	 * 打开前置或后置摄像头
	 */
	public Camera getCameraSafely(int cameraId) {
		Camera camera = null;
		try {
			camera = Camera.open(cameraId);
		} catch (Exception e) {
			camera = null;
		}
		return camera;
	}

	public RelativeLayout.LayoutParams getParams(Camera camera) {
		Camera.Parameters camPara = camera.getParameters();
		// Log.w("ceshi", "Screen.mWidth===" + Screen.mWidth +
		// ", Screen.mHeight===" + Screen.mHeight);
		// 注意Screen是否初始化
		Camera.Size bestPreviewSize = calBestPreviewSize(camPara,
				Screen.mWidth, Screen.mHeight);
		cameraWidth = bestPreviewSize.width;
		cameraHeight = bestPreviewSize.height;
		camPara.setPreviewSize(cameraWidth, cameraHeight);
		camera.setParameters(camPara);

		float scale = bestPreviewSize.width / bestPreviewSize.height;

		RelativeLayout.LayoutParams layoutPara = new RelativeLayout.LayoutParams(
				(int) (bestPreviewSize.width),
				(int) (bestPreviewSize.width / scale));

		layoutPara.addRule(RelativeLayout.CENTER_HORIZONTAL);// 设置照相机水平居中
		return layoutPara;
	}

	public Bitmap getBitMap(byte[] data, Camera camera, boolean mIsFrontalCamera) {
		int width = camera.getParameters().getPreviewSize().width;
		int height = camera.getParameters().getPreviewSize().height;
		YuvImage yuvImage = new YuvImage(data, camera.getParameters()
				.getPreviewFormat(), width, height, null);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80,
				byteArrayOutputStream);
		byte[] jpegData = byteArrayOutputStream.toByteArray();
		// 获取照相后的bitmap
		Bitmap tmpBitmap = BitmapFactory.decodeByteArray(jpegData, 0,
				jpegData.length);
		Matrix matrix = new Matrix();
		matrix.reset();
		if (mIsFrontalCamera) {
			matrix.setRotate(-90);
		} else {
			matrix.setRotate(90);
		}
		tmpBitmap = Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(),
				tmpBitmap.getHeight(), matrix, true);
		tmpBitmap = tmpBitmap.copy(Bitmap.Config.ARGB_8888, true);

		int hight = tmpBitmap.getHeight() > tmpBitmap.getWidth() ? tmpBitmap
				.getHeight() : tmpBitmap.getWidth();

		float scale = hight / 800.0f;

		if (scale > 1) {
			tmpBitmap = Bitmap.createScaledBitmap(tmpBitmap,
					(int) (tmpBitmap.getWidth() / scale),
					(int) (tmpBitmap.getHeight() / scale), false);
		}
		return tmpBitmap;
	}

	/**
	 *获取照相机旋转角度 
	 */
	public int getCameraAngle(Activity activity) {
		int rotateAngle = 90;
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
			rotateAngle = (info.orientation + degrees) % 360;
			rotateAngle = (360 - rotateAngle) % 360; // compensate the mirror
		} else { // back-facing
			rotateAngle = (info.orientation - degrees + 360) % 360;
		}
		return rotateAngle;
	}
}