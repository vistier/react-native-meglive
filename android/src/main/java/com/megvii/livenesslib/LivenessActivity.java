package com.megvii.livenesslib;

import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.megvii.livenessdetection.DetectionConfig;
import com.megvii.livenessdetection.DetectionFrame;
import com.megvii.livenessdetection.Detector;
import com.megvii.livenessdetection.Detector.DetectionFailedType;
import com.megvii.livenessdetection.Detector.DetectionListener;
import com.megvii.livenessdetection.Detector.DetectionType;
import com.megvii.livenessdetection.FaceQualityManager;
import com.megvii.livenessdetection.FaceQualityManager.FaceQualityErrorType;
import com.megvii.livenessdetection.bean.FaceInfo;
import com.megvii.livenesslib.R;
import com.megvii.livenesslib.util.ConUtil;
import com.megvii.livenesslib.util.DialogUtil;
import com.megvii.livenesslib.util.ICamera;
import com.megvii.livenesslib.util.IDetection;
import com.megvii.livenesslib.util.IFile;
import com.megvii.livenesslib.util.IMediaPlayer;
import com.megvii.livenesslib.util.Screen;
import com.megvii.livenesslib.util.SensorUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class LivenessActivity extends Activity implements PreviewCallback,
		DetectionListener, TextureView.SurfaceTextureListener {

	private TextureView camerapreview;
	private FaceMask mFaceMask;// 画脸位置的类（调试时会用到）
	private ProgressBar mProgressBar;// 网络上传请求验证时出现的ProgressBar
	private LinearLayout headViewLinear;// "请在光线充足的情况下进行检测"这个视图
	private RelativeLayout rootView;// 根视图
	private TextView timeOutText;
	private LinearLayout timeOutLinear;

	private Detector mDetector;// 实体检测器
	private Handler mainHandler;
	private JSONObject jsonObject;
	private IMediaPlayer mIMediaPlayer;// 多媒体工具类
	private ICamera mICamera;// 照相机工具类
	private IFile mIFile;// 文件工具类
	private IDetection mIDetection;
	private DialogUtil mDialogUtil;

	private TextView promptText;
	private boolean isHandleStart;// 是否开始检测
	private Camera mCamera;
	private String mSession;
	private FaceQualityManager mFaceQualityManager;
	private SensorUtil sensorUtil;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.liveness_layout);
		init();
		initData();
	}

	private void init() {
		sensorUtil = new SensorUtil(this);
		Screen.initialize(this);
		mSession = ConUtil.getFormatterTime(System.currentTimeMillis());
		mainHandler = new Handler();
		mIMediaPlayer = new IMediaPlayer(this);
		mIFile = new IFile();
		mDialogUtil = new DialogUtil(this);
		rootView = (RelativeLayout) findViewById(R.id.liveness_layout_rootRel);
		mIDetection = new IDetection(this, rootView);
		mFaceMask = (FaceMask) findViewById(R.id.liveness_layout_facemask);
		mICamera = new ICamera();
		promptText = (TextView) findViewById(R.id.liveness_layout_promptText);
		camerapreview = (TextureView) findViewById(R.id.liveness_layout_textureview);
		camerapreview.setSurfaceTextureListener(this);
		mProgressBar = (ProgressBar) findViewById(R.id.liveness_layout_progressbar);
		mProgressBar.setVisibility(View.INVISIBLE);
		headViewLinear = (LinearLayout) findViewById(R.id.liveness_layout_bottom_tips_head);
		headViewLinear.setVisibility(View.VISIBLE);
		timeOutLinear = (LinearLayout) findViewById(R.id.detection_step_timeoutLinear);
		timeOutText = (TextView) findViewById(R.id.detection_step_timeout);

		mIDetection.viewsInit();
	}

	/**
	 * 初始化数据
	 */
	private void initData() {
		DetectionConfig config = new DetectionConfig.Builder().build();
		mDetector = new Detector(this, config);
		int initSuccess = mDetector.init(this, ConUtil.readModel(this), "", "" );

		Log.w("Pansky", " ===>  "+ initSuccess);

		if (initSuccess != 0) {
			mDialogUtil.showDialog("检测器初始化失败");
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				mIDetection.animationInit();
			}
		}).start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		isHandleStart = false;
		mCamera = mICamera.openCamera(this);
		if (mCamera != null) {
			CameraInfo cameraInfo = new CameraInfo();
			Camera.getCameraInfo(1, cameraInfo);
			mFaceMask
					.setFrontal(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT);
			RelativeLayout.LayoutParams layout_params = mICamera
					.getLayoutParam();
			camerapreview.setLayoutParams(layout_params);
			mFaceMask.setLayoutParams(layout_params);
			mFaceQualityManager = new FaceQualityManager(1 - 0.5f, 0.5f);
			mIDetection.mCurShowIndex = -1;
		} else {
			mDialogUtil.showDialog("打开前置摄像头失败");
		}
	}

	/**
	 * 开始检测
	 */
	private void handleStart() {
		if (isHandleStart)
			return;
		isHandleStart = true;
		Animation animationIN = AnimationUtils.loadAnimation(
				LivenessActivity.this, R.anim.liveness_rightin);
		Animation animationOut = AnimationUtils.loadAnimation(
				LivenessActivity.this, R.anim.liveness_leftout);
		headViewLinear.startAnimation(animationOut);
		mIDetection.mAnimViews[0].setVisibility(View.VISIBLE);
		mIDetection.mAnimViews[0].startAnimation(animationIN);
		animationOut.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				timeOutLinear.setVisibility(View.VISIBLE);
			}
		});
		mainHandler.post(mTimeoutRunnable);

		try {
			jsonObject = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			jsonObject.put("imgs", jsonArray);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private Runnable mTimeoutRunnable = new Runnable() {
		@Override
		public void run() {
			// 倒计时开始
			initDetecteSession();
			if (mIDetection.mDetectionSteps != null)
				changeType(mIDetection.mDetectionSteps.get(0), 10);
		}
	};

	private void initDetecteSession() {
		if (mICamera.mCamera == null)
			return;

		mProgressBar.setVisibility(View.INVISIBLE);
		mIDetection.detectionTypeInit();

		mCurStep = 0;
		mDetector.reset();
		mDetector.changeDetectionType(mIDetection.mDetectionSteps.get(0));
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		Size previewsize = camera.getParameters().getPreviewSize();
		mDetector.doDetection(data, previewsize.width, previewsize.height,
				360 - mICamera.getCameraAngle(this));
	}

	/**
	 * 实体验证成功
	 */
	@Override
	public DetectionType onDetectionSuccess(final DetectionFrame validFrame) {
		mIMediaPlayer.reset();
		mCurStep++;
		mFaceMask.setFaceInfo(null);

		if (mCurStep >= mIDetection.mDetectionSteps.size()) {
			mProgressBar.setVisibility(View.VISIBLE);
			handleResult(R.string.verify_success);
		} else
			changeType(mIDetection.mDetectionSteps.get(mCurStep), 10);

		// 检测器返回值：如果不希望检测器检测则返回DetectionType.DONE，如果希望检测器检测动作则返回要检测的动作
		return mCurStep >= mIDetection.mDetectionSteps.size() ? DetectionType.DONE
				: mIDetection.mDetectionSteps.get(mCurStep);
	}

	/**
	 * 活体检测失败
	 */
	@Override
	public void onDetectionFailed(final DetectionFailedType type) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				mIFile.saveLog(mSession, type.name());
			}
		}).start();
		int resourceID = R.string.liveness_detection_failed;
		switch (type) {
		case ACTIONBLEND:
			resourceID = R.string.liveness_detection_failed_action_blend;
			break;
		case NOTVIDEO:
			resourceID = R.string.liveness_detection_failed_not_video;
			break;
		case TIMEOUT:
			resourceID = R.string.liveness_detection_failed_timeout;
			break;
		}
		handleResult(resourceID);
	}

	/**
	 * 活体验证中
	 */
	@Override
	public void onFrameDetected(long timeout, DetectionFrame detectionFrame) {
		if (sensorUtil.isVertical()) {
			faceOcclusion(detectionFrame);
			handleNotPass(timeout);
			mFaceMask.setFaceInfo(detectionFrame);
		} else
			promptText.setText("请竖直握紧手机");
	}

	private void faceOcclusion(DetectionFrame detectionFrame) {
		mFailFrame++;
		if (detectionFrame != null) {
			FaceInfo faceInfo = detectionFrame.getFaceInfo();
			if (faceInfo != null) {
				if (faceInfo.eyeLeftOcclusion > 0.5
						|| faceInfo.eyeRightOcclusion > 0.5) {
					if (mFailFrame > 10) {
						mFailFrame = 0;
						promptText.setText("请勿用手遮挡眼睛");
					}
					return;
				}
				if (faceInfo.mouthOcclusion > 0.5) {
					if (mFailFrame > 10) {
						mFailFrame = 0;
						promptText.setText("请勿用手遮挡嘴巴");
					}
					return;
				}
			}
		}
		faceInfoChecker(mFaceQualityManager.feedFrame(detectionFrame));
	}

	private int mFailFrame = 0;

	public void faceInfoChecker(List<FaceQualityErrorType> errorTypeList) {
		if (errorTypeList == null || errorTypeList.size() == 0)
			handleStart();
		else {
			String infoStr = "";
			FaceQualityErrorType errorType = errorTypeList.get(0);
			if (errorType == FaceQualityErrorType.FACE_NOT_FOUND) {
				infoStr = "请让我看到您的正脸";
			} else if (errorType == FaceQualityErrorType.FACE_POS_DEVIATED) {
				infoStr = "请让我看到您的正脸";
			} else if (errorType == FaceQualityErrorType.FACE_NONINTEGRITY) {
				infoStr = "请让我看到您的正脸";
			} else if (errorType == FaceQualityErrorType.FACE_TOO_DARK) {
				infoStr = "请让光线再亮点";
			} else if (errorType == FaceQualityErrorType.FACE_TOO_BRIGHT) {
				infoStr = "请让光线再暗点";
			} else if (errorType == FaceQualityErrorType.FACE_TOO_SMALL) {
				infoStr = "请再靠近一些";
			} else if (errorType == FaceQualityErrorType.FACE_TOO_LARGE) {
				infoStr = "请再离远一些";
			} else if (errorType == FaceQualityErrorType.FACE_TOO_BLURRY) {
				infoStr = "请避免侧光和背光";
			} else if (errorType == FaceQualityErrorType.FACE_OUT_OF_RECT) {
				infoStr = "请保持脸在人脸框中";
			}

			// mFailFrame++;
			if (mFailFrame > 10) {
				mFailFrame = 0;
				promptText.setText(infoStr);
			}
		}
	}

	/**
	 * 跳转Activity传递信息
	 */
	private void handleResult(final int resID) {
		String resultString = getResources().getString(resID);
		try {
			jsonObject.put("result", resultString);
			jsonObject.put("resultcode", resID);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Intent intent = new Intent();
		intent.putExtra("result", jsonObject.toString());
		setResult(RESULT_OK, intent);
		finish();
	}

	private int mCurStep = 0;// 检测动作的次数

	public void changeType(final Detector.DetectionType detectiontype,
			long timeout) {
		mIDetection.changeType(detectiontype, timeout);
		mFaceMask.setFaceInfo(null);

		if (mCurStep == 0) {
			mIMediaPlayer.doPlay(mIMediaPlayer.getSoundRes(detectiontype));
		} else {
			mIMediaPlayer.doPlay(R.raw.meglive_well_done);
			mIMediaPlayer.setOnCompletionListener(detectiontype);
		}
	}

	public void handleNotPass(final long remainTime) {
		if (remainTime > 0) {
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					timeOutText.setText(remainTime / 1000 + "");
				}
			});
		}
	}

	private boolean mHasSurface = false;

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
										  int height) {
		mHasSurface = true;
		doPreview();

		// 添加活体检测回调
		mDetector.setDetectionListener(this);
		mICamera.actionDetect(this);
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
											int height) {
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		mHasSurface = false;
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
	}

	private void doPreview() {
		if (!mHasSurface)
			return;

		mICamera.startPreview(camerapreview.getSurfaceTexture());
	}

	@Override
	protected void onPause() {
		super.onPause();
		mainHandler.removeCallbacksAndMessages(null);
		mICamera.closeCamera();
		mCamera = null;
		mIMediaPlayer.close();

		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mDetector != null)
			mDetector.release();
		mDialogUtil.onDestory();
		mIDetection.onDestroy();
		sensorUtil.release();
	}
}