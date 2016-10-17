package com.mr235.videodemo;

import android.content.Context;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Button btn_VideoStart, btn_VideoStop;
    private SurfaceView sv_view;
    private boolean isRecording;
    private MediaRecorder mediaRecorder;
    private TextView mTvTime;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_VideoStart = (Button) findViewById(R.id.btn_VideoStart);
        btn_VideoStop = (Button) findViewById(R.id.btn_VideoStop);
        sv_view = (SurfaceView) findViewById(R.id.sv_view);
        mTvTime = (TextView) findViewById(R.id.tv_time);

        btn_VideoStop.setEnabled(false);

        btn_VideoStart.setOnClickListener(click);
        btn_VideoStop.setOnClickListener(click);

        // 声明Surface不维护自己的缓冲区，针对Android3.0以下设备支持
        sv_view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private long startTime = 0l;
    private long delayedTime = 200l;
    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            mTvTime.setText(getTimeStr());
            mHandler.postDelayed(timeRunnable, delayedTime);
        }
    };

    private String getTimeStr() {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - startTime;
        String timeStr = "";
        if (diff/1000>60) {
            long minute = diff / 1000 / 60;
            long second = (diff - minute * 1000 * 60) / 1000;
            timeStr = String.format("%02d:%02d", minute, second);
        } else {
            long second = diff / 1000;
            timeStr = String.format("00:%02d", second);
        }
        return timeStr;
    }

    private View.OnClickListener click = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_VideoStart:
                    start();
                    break;
                case R.id.btn_VideoStop:
                    stop();
                    break;
                default:
                    break;
            }
        }
    };

    protected void start() {
        try {
            long l = System.currentTimeMillis();
            SimpleDateFormat format = new SimpleDateFormat("MMdd-HH-mm-ss");
            File file = new File("/sdcard/aavideo/", format.format(new Date()) + ".mp4");
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.exists()) {
                // 如果文件存在，删除它，演示代码保证设备上只有一个录音文件
                file.delete();
            }

            mediaRecorder = new MediaRecorder();
            mediaRecorder.reset();

            //设置摄像头以及摄像头的方向
            int CammeraIndex=FindBackCamera();//网上参考的一个函数，用来获取后置摄像头的info
            Camera mCamera= Camera.open(CammeraIndex);
            // 设置旋转角度
            mCamera.setDisplayOrientation(getDisplayOrientation());
            Camera.Parameters parameters = mCamera.getParameters();
            int sv_viewWidth = sv_view.getWidth();
            int sv_viewHeight = sv_view.getHeight();
            Camera.Size previewSize = parameters.getPreviewSize();
            ViewGroup.LayoutParams lp = sv_view.getLayoutParams();
            lp.height = previewSize.width;
            lp.width = previewSize.height;
            sv_view.setLayoutParams(lp);
            mCamera.setParameters(parameters);
            mCamera.unlock();
            mediaRecorder.setCamera(mCamera);

            System.out.println(previewSize.width + "*" + previewSize.height + "  " + sv_view.getWidth() + "*" + sv_view.getHeight());

            // 设置音频录入源
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // 设置视频图像的录入源
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            // 设置录入媒体的输出格式
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            // 设置音频的编码格式
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            // 设置视频的编码格式
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
            mediaRecorder.setVideoSize(800, 480);
            //提高帧频率，录像模糊，花屏，绿屏可写上调试
            mediaRecorder.setVideoEncodingBitRate(5*1024*1024);
            // 设置视频的采样率，每秒4帧
            mediaRecorder.setVideoFrameRate(15);
            // 设置录制视频文件的输出路径
            mediaRecorder.setOutputFile(file.getAbsolutePath());
            // 设置捕获视频图像的预览界面
            mediaRecorder.setPreviewDisplay(sv_view.getHolder().getSurface());
            // 设置旋转角度
            mediaRecorder.setOrientationHint(getDisplayOrientation());


            mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {

                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    // 发生错误，停止录制
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
                    isRecording=false;
                    btn_VideoStart.setEnabled(true);
                    btn_VideoStop.setEnabled(false);
                    Toast.makeText(MainActivity.this, "录制出错", Toast.LENGTH_SHORT).show();
                    mHandler.removeCallbacks(timeRunnable);
                }
            });

            // 准备、开始
            mediaRecorder.prepare();
            mediaRecorder.start();

            btn_VideoStart.setEnabled(false);
            btn_VideoStop.setEnabled(true);
            isRecording = true;
            Toast.makeText(this, "开始录像", Toast.LENGTH_SHORT).show();
            mHandler.removeCallbacks(timeRunnable);
            startTime = System.currentTimeMillis();
            mHandler.postDelayed(timeRunnable, delayedTime);
        } catch (Exception e) {
            e.printStackTrace();
            mHandler.removeCallbacks(timeRunnable);
        }

    }
    private int FindFrontCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_FRONT ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }
    //判断后置摄像头是否存在
    private int FindBackCamera(){
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number

        for ( int camIdx = 0; camIdx < cameraCount;camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo ); // get camerainfo
            if ( cameraInfo.facing ==Camera.CameraInfo.CAMERA_FACING_BACK ) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }
    public int getDisplayOrientation() {
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
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

        android.hardware.Camera.CameraInfo camInfo =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);

        int result = (camInfo.orientation - degrees + 360) % 360;
        return result;
    }

    protected void stop() {
        if (isRecording) {
            // 如果正在录制，停止并释放资源
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording=false;
            btn_VideoStart.setEnabled(true);
            btn_VideoStop.setEnabled(false);
            Toast.makeText(this, "停止录像，并保存文件", Toast.LENGTH_SHORT).show();
        }
        mHandler.removeCallbacks(timeRunnable);
    }

    @Override
    protected void onDestroy() {
        if (isRecording) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        super.onDestroy();
    }

}