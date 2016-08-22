package com.megvii.livenesslib.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.megvii.livenesslib.R;
import com.pansky.megvill.MegliveReactPackage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ConUtil {
	
	public static void copyModels(Context context) {
		File dstModelFile = new File(context.getExternalFilesDir(null), "model");
		if (dstModelFile.exists()) {
			return;
		}

		try {
			String tmpFile = "model";
			BufferedInputStream inputStream = new BufferedInputStream(context
					.getAssets().open(tmpFile));
			BufferedOutputStream foutputStream = new BufferedOutputStream(
					new FileOutputStream(dstModelFile));

			byte[] buffer = new byte[1024];
			int readcount = -1;
			while ((readcount = inputStream.read(buffer)) != -1) {
				foutputStream.write(buffer, 0, readcount);
			}
			foutputStream.close();
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] readModel(Context context) {
		InputStream inputStream = null;
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int count = -1;
		try {
			inputStream = context.getResources().openRawResource(MegliveReactPackage.RES_MODEL);
			while ((count = inputStream.read(buffer)) != -1) {
				byteArrayOutputStream.write(buffer, 0, count);
			}
			byteArrayOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return byteArrayOutputStream.toByteArray();
	}
	
	public static String getUUIDString(Context mContext) {
		String KEY_UUID = "key_uuid";
		SharedUtil sharedUtil = new SharedUtil(mContext);
		String uuid = sharedUtil.getStringValueByKey(KEY_UUID);
		if (uuid != null)
			return uuid;

		uuid = getPhoneNumber(mContext);
		if (uuid == null || uuid.trim().length() == 0) {
			uuid = getMacAddress(mContext);
			if (uuid == null || uuid.trim().length() == 0) {
				uuid = getDeviceID(mContext);
				if (uuid == null || uuid.trim().length() == 0) {
					uuid = UUID.randomUUID().toString();
					uuid = Base64.encodeToString(uuid.getBytes(), Base64.DEFAULT);
				}
			}
		}
		sharedUtil.saveStringValue(KEY_UUID, uuid);
		return uuid;
	}

	public static String getPhoneNumber(Context mContext) {
		TelephonyManager phoneMgr = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		return phoneMgr.getLine1Number();
	}

	public static String getDeviceID(Context mContext) {
		TelephonyManager tm = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();
	}

	public static String getMacAddress(Context mContext) {
		WifiManager wifi = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifi.getConnectionInfo();
		String address = info.getMacAddress();
		if(address != null && address.length() > 0){
			address = address.replace(":", "");
		}
		return address;
	}
	
	/**
	 * 获取bitmap的灰度图像
	 */
	public static byte[] getGrayscale(Bitmap bitmap) {
		if (bitmap == null)
			return null;

		byte[] ret = new byte[bitmap.getWidth() * bitmap.getHeight()];
		for (int j = 0; j < bitmap.getHeight(); ++j)
			for (int i = 0; i < bitmap.getWidth(); ++i) {
				int pixel = bitmap.getPixel(i, j);
				int red = ((pixel & 0x00FF0000) >> 16);
				int green = ((pixel & 0x0000FF00) >> 8);
				int blue = pixel & 0x000000FF;
				ret[j * bitmap.getWidth() + i] = (byte) ((299 * red + 587
						* green + 114 * blue) / 1000);
			}
		return ret;
	}

	/**
	 * 读取图片属性：旋转的角度
	 * 
	 * @param path
	 *            图片绝对路径
	 * @return degree旋转的角度
	 */
	public static int readPictureDegree(String path) {
		int degree = 0;
		try {
			ExifInterface exifInterface = new ExifInterface(path);
			int orientation = exifInterface.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				degree = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				degree = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				degree = 270;
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return degree;
	}

	/**
	 * 旋转图片
	 * 
	 * @param angle
	 * @param bitmap
	 * @return Bitmap
	 */
	public static Bitmap rotateImage(int angle, Bitmap bitmap) {
		// 图片旋转矩阵
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		// 得到旋转后的图片
		Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
				bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		return resizedBitmap;
	}

	private static Bitmap getBitMap(String fileSrc, int dstWidth) {
		if (dstWidth == -1) {
			return BitmapFactory.decodeFile(fileSrc);
		}
		// 获取图片的宽和高
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(fileSrc, options);

		// 压缩图片
		options.inSampleSize = Math.max(1, (int) (Math.max(
				(double) options.outWidth / dstWidth,
				(double) options.outHeight / dstWidth)));
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(fileSrc, options);
	}

	/**
	 * 压缩图
	 */
	public static Bitmap getBitmapConsiderExif(String imagePath) {
		
		// 获取照相后的bitmap
		//Bitmap tmpBitmap = BitmapFactory.decodeFile(imagePath);
		Bitmap tmpBitmap = getBitMap(imagePath, 800);
		if (tmpBitmap == null)
			return null;
		Matrix matrix = new Matrix();
		matrix.postRotate(readPictureDegree(imagePath));
		tmpBitmap = Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(),
				tmpBitmap.getHeight(), matrix, true);
		tmpBitmap = tmpBitmap.copy(Config.ARGB_8888, true);

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
	 * 切图
	 */
	public static Bitmap cropImage(RectF rect, Bitmap bitmap) {
		float width = rect.width() * 2;
		if (width > bitmap.getWidth()) {
			width = bitmap.getWidth();
		}

		float hight = rect.height() * 2;
		if (hight > bitmap.getHeight()) {
			hight = bitmap.getHeight();
		}

		float l = rect.centerX() - (width / 2);
		if (l < 0) {
			l = 0;
		}
		float t = rect.centerY() - (hight / 2);
		if (t < 0) {
			t = 0;
		}
		if (l + width > bitmap.getWidth()) {
			width = bitmap.getWidth() - l;
		}
		if (t + hight > bitmap.getHeight()) {
			hight = bitmap.getHeight() - t;
		}

		return Bitmap.createBitmap(bitmap, (int) l, (int) t, (int) width,
				(int) hight);

	}

	/**
	 * 切图
	 */
	public static Bitmap cutImage(RectF rect, String imagePath) {
		Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
		return cropImage(rect, bitmap);

	}

	/**
	 * 照相机拍照后照片存储路径
	 */
	public static File getOutputMediaFile(Context mContext) {
		File mediaStorageDir = mContext
				.getExternalFilesDir(Constant.cacheCampareImage);
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile;

		mediaFile = new File(mediaStorageDir.getPath() + File.separator
				+ "IMG_" + timeStamp + ".jpg");
		return mediaFile;
	}

	/**
	 * 隐藏软键盘
	 */
	public static void isGoneKeyBoard(Activity activity) {
		if (activity.getCurrentFocus() != null) {
			// 隐藏软键盘
			((InputMethodManager) activity
					.getSystemService(activity.INPUT_METHOD_SERVICE))
					.hideSoftInputFromWindow(activity.getCurrentFocus()
							.getWindowToken(),
							InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	/**
	 * 输出toast
	 */
	public static void showToast(Context context, String str) {
		if (context != null) {
			Toast toast = Toast.makeText(context, str, Toast.LENGTH_SHORT);
			// 可以控制toast显示的位置
			toast.setGravity(Gravity.TOP, 0, 30);
			toast.show();
		}
	}

	/**
	 * 输出长时间toast
	 */
	public static void showLongToast(Context context, String str) {
		if (context != null) {
			Toast toast = Toast.makeText(context, str, Toast.LENGTH_LONG);
			// 可以控制toast显示的位置
			toast.setGravity(Gravity.TOP, 0, 30);
			toast.show();
		}
	}

	/**
	 * 获取APP版本名
	 */
	public static String getVersionName(Context context) {
		try {
			String versionName = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
			return versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 镜像旋转
	 */
	public static Bitmap convert(Bitmap bitmap, boolean mIsFrontalCamera) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Bitmap newbBitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
		Canvas cv = new Canvas(newbBitmap);
		Matrix m = new Matrix();
		// m.postScale(1, -1); //镜像垂直翻转
		if (mIsFrontalCamera) {
			m.postScale(-1, 1); // 镜像水平翻转
		}
		// m.postRotate(-90); //旋转-90度
		Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, w, h, m, true);
		cv.drawBitmap(bitmap2,
				new Rect(0, 0, bitmap2.getWidth(), bitmap2.getHeight()),
				new Rect(0, 0, w, h), null);
		return newbBitmap;
	}

	/**
	 * 保存bitmap至指定Picture文件夹
	 */
	public static String saveBitmap(Context mContext, Bitmap bitmaptosave) {
		if (bitmaptosave == null)
			return null;

		File mediaStorageDir = mContext
				.getExternalFilesDir(Constant.cacheImage);

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}
		// String bitmapFileName = System.currentTimeMillis() + ".jpg";
		String bitmapFileName = System.currentTimeMillis() + "";
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(mediaStorageDir + "/" + bitmapFileName);
			boolean successful = bitmaptosave.compress(
					Bitmap.CompressFormat.JPEG, 75, fos);

			if (successful)
				return mediaStorageDir.getAbsolutePath() + "/" + bitmapFileName;
			else
				return null;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 时间格式化(格式到秒)
	 */
	public static String getFormatterTime(long time) {
		Date d = new Date(time);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		String data = formatter.format(d);
		return data;
	}
}
