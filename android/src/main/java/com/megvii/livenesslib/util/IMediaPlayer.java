package com.megvii.livenesslib.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import com.megvii.livenessdetection.Detector;
import com.megvii.livenesslib.R;

public class IMediaPlayer {
    public MediaPlayer mMediaPlayer;
    private Context mContext;
    
    public IMediaPlayer(Context mContext) {
        this.mContext = mContext;
        mMediaPlayer = new MediaPlayer();
    }

    public void close(){
    	mContext = null;
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
    
    /**
     *重置 （重新设置播放器引擎）
     */
    public void reset(){
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
    }
    
    /**
     *播放成功回调接口 
     */
    public void setOnCompletionListener(final Detector.DetectionType detectiontype){
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                doPlay(getSoundRes(detectiontype));
                mMediaPlayer.setOnCompletionListener(null);
            }
        });
    }
    
    /**
     * 多媒体播放
     */
    public void doPlay(int rawId) {
        if (this.mMediaPlayer == null)
            return;
        mMediaPlayer.reset();
        try {
            AssetFileDescriptor localAssetFileDescriptor = mContext.getResources().openRawResourceFd(rawId);
            mMediaPlayer.setDataSource(localAssetFileDescriptor.getFileDescriptor(),
                    localAssetFileDescriptor.getStartOffset(),
                    localAssetFileDescriptor.getLength());
            localAssetFileDescriptor.close();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
            mMediaPlayer.prepareAsync();
        } catch (Exception localIOException) {
            localIOException.printStackTrace();
        }
    }
    
    public int getSoundRes(Detector.DetectionType detectionType) {
        int resID = -1;
        switch (detectionType) {
            case POS_PITCH:
                resID = R.raw.meglive_pitch_down;
                break;
            case POS_YAW_LEFT:
            case POS_YAW_RIGHT:
            case POS_YAW:
                resID = R.raw.meglive_yaw;
                break;
            case MOUTH:
                resID = R.raw.meglive_mouth_open;
                break;
            case BLINK:
                resID = R.raw.meglive_eye_blink;
                break;
        }
        return resID;
    }
}