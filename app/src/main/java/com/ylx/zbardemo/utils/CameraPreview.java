package com.ylx.zbardemo.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by HJK on 16/7/15.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.PreviewCallback mPreviewCallBack;
    private Camera.AutoFocusCallback mAutoFocusCallBack;

    public CameraPreview(Context context, Camera camera,
                         Camera.PreviewCallback previewCb, Camera.AutoFocusCallback autoFocusCb) {
        super(context);
        mCamera = camera;
        mPreviewCallBack = previewCb;
        mAutoFocusCallBack = autoFocusCb;

		/*
         * 自动聚焦 要求API版本>9
		 */
        Camera.Parameters parameters = camera.getParameters();
        for (String f : parameters.getSupportedFocusModes()) {
            if (f == Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                mAutoFocusCallBack = null;
                break;
            }
        }
        if (parameters != null) {
            parameters.setZoom(parameters.getMaxZoom() * 2 / 3);
            camera.setParameters(parameters);
        }

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the
        // preview.
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.d("DBG", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Camera preview released in activity
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
		/*
		 * If your preview can change or rotate, take care of those events here.
		 * Make sure to stop the preview before resizing or reformatting it.
		 */
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        try {
            // Hard code camera surface rotation 90 degs to match Activity view
            // in portrait
            mCamera.setDisplayOrientation(90);

            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(mPreviewCallBack);
            mCamera.startPreview();
            mCamera.autoFocus(mAutoFocusCallBack);
        } catch (Exception e) {
            Log.d("DBG", "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    protected void onDraw(Canvas mCanvas) {
    }
}
