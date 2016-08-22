package com.megvii.livenesslib.util;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.megvii.livenessdetection.Detector;
import com.megvii.livenessdetection.Detector.DetectionType;
import com.megvii.livenesslib.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * 实体验证工具类
 */
public class IDetection {

	private View rootView;
	private Context mContext;

	public View[] mAnimViews;
	private int num = 3;// 动作数量
	private HashMap<Integer, Drawable> mDrawableCache;
	public int mCurShowIndex = -1;// 现在底部展示试图的索引值
	public ArrayList<Detector.DetectionType> mDetectionSteps;// 活体检测动作列表

	public IDetection(Context mContext, View view) {
		this.mContext = mContext;
		this.rootView = view;
		mDrawableCache = new HashMap<Integer, Drawable>();
	}

	public void animationInit() {
		int[] reses = { R.drawable.liveness_head_pitch, R.drawable.liveness_head_yaw,
				R.drawable.liveness_mouth_open_closed, R.drawable.liveness_eye_open_closed };
		for (int oneRes : reses) {
			mDrawableCache.put(oneRes,
					(mContext.getResources().getDrawable(oneRes)));
		}
	}

	public void viewsInit() {
		mAnimViews = new View[2];
		mAnimViews[0] = (rootView.findViewById(R.id.liveness_layout_first_layout));
		mAnimViews[1] = (rootView.findViewById(R.id.liveness_layout_second_layout));
		for (View tmpView : mAnimViews) {
			tmpView.setVisibility(View.INVISIBLE);
		}
	}

	public void changeType(final Detector.DetectionType detectiontype,
			long timeout) {
		Animation animationIN = AnimationUtils.loadAnimation(mContext,
				R.anim.liveness_rightin);
		Animation animationOut = AnimationUtils.loadAnimation(mContext,
				R.anim.liveness_leftout);

		if (mCurShowIndex != -1) // 已经存在layout 需要移除之
		{
			mAnimViews[mCurShowIndex].setVisibility(View.INVISIBLE);
			mAnimViews[mCurShowIndex].setAnimation(animationOut);
		} else {
			mAnimViews[0].setVisibility(View.INVISIBLE);
			mAnimViews[0].startAnimation(animationOut);
		}

		mCurShowIndex = mCurShowIndex == -1 ? 0 : (mCurShowIndex == 0 ? 1 : 0);
		initAnim(detectiontype, mAnimViews[mCurShowIndex]);
		mAnimViews[mCurShowIndex].setVisibility(View.VISIBLE);
		mAnimViews[mCurShowIndex].startAnimation(animationIN);
	}

	private void initAnim(Detector.DetectionType detectiontype, View layoutView) {
		ImageView tmpImageView = (ImageView) layoutView
				.findViewById(R.id.detection_step_image);
		tmpImageView.setImageDrawable(getDrawRes(detectiontype));

		((AnimationDrawable) tmpImageView.getDrawable()).start();
		TextView detectionName = (TextView) layoutView
				.findViewById(R.id.detection_step_name);
		detectionName.setText(getDetectionName(detectiontype));
	}

	private Drawable getDrawRes(Detector.DetectionType detectionType) {
		int resID = -1;
		switch (detectionType) {
		case POS_PITCH:
		case POS_PITCH_UP:
		case POS_PITCH_DOWN:
			resID = R.drawable.liveness_head_pitch;
			break;
		case POS_YAW_LEFT:
		case POS_YAW_RIGHT:
		case POS_YAW:
			resID = R.drawable.liveness_head_yaw;
			break;
		case MOUTH:
			resID = R.drawable.liveness_mouth_open_closed;
			break;
		case BLINK:
			resID = R.drawable.liveness_eye_open_closed;
			break;
		}
		Drawable cachedDrawAble = mDrawableCache.get(resID);
		if (cachedDrawAble != null)
			return cachedDrawAble;
		else {
			Drawable drawable = mContext.getResources().getDrawable(resID);
			mDrawableCache.put(resID, (drawable));
			return drawable;
		}
	}

	private String getDetectionName(Detector.DetectionType detectionType) {
		String detectionName = null;
		switch (detectionType) {
		case POS_PITCH:
			detectionName = "缓慢点头";
			break;
		case POS_YAW:
			detectionName = "左右摇头";
			break;
		case MOUTH:
			detectionName = "张嘴";
			break;
		case BLINK:
			detectionName = "眨眼";
			break;
		case POS_YAW_LEFT:
			detectionName = "左转";
			break;
		case POS_YAW_RIGHT:
			detectionName = "右转";
			break;
		}
		return detectionName;
	}

	/**
	 * 初始化检测动作
	 */
	public void detectionTypeInit() {
		ArrayList<DetectionType> tmpTypes = new ArrayList<Detector.DetectionType>();
		tmpTypes.add(Detector.DetectionType.BLINK);// 眨眼
		tmpTypes.add(Detector.DetectionType.MOUTH);// 张嘴
		tmpTypes.add(Detector.DetectionType.POS_PITCH);// 缓慢点头
		tmpTypes.add(Detector.DetectionType.POS_YAW);// 左右摇头

		Collections.shuffle(tmpTypes);// 打乱顺序

		mDetectionSteps = new ArrayList<DetectionType>(num);
		for (int i = 0; i < num; i++) {
			mDetectionSteps.add(tmpTypes.get(i));
		}
	}

	public void onDestroy() {
		rootView = null;
		mContext = null;
		if (mDrawableCache != null) {
			mDrawableCache.clear();
		}
	}
}
