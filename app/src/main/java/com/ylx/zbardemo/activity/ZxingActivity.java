package com.ylx.zbardemo.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ylx.zbardemo.R;
import com.ylx.zbardemo.utils.BitmapUtils;
import com.ylx.zbardemo.utils.CameraPreview;
import com.ylx.zbardemo.utils.ScanUtils;
import com.ylx.zbardemo.utils.StringHelper;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

public class ZxingActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 123;
    private TextView mTopRightText;
    private FrameLayout mPreViewLayout;
    private ImageView mScanLineImage;
    private TextView mScanContentText;
    private TextView mContinueScanText;

    // 相机
    private Camera mCamera;
    // 预览视图
    private CameraPreview mPreview;
    // 自动聚焦
    private Handler mAutoFocusHandler;
    // 图片扫描器
    private ImageScanner mScanner;
    // 是否扫描完毕
    private boolean isScanned = false;
    // 是否处于预览状态
    private boolean isPreview = true;

    private TranslateAnimation mTranslateAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zxing);
        initView();
        regListener();
        loadData();
    }

    private void initView() {
        mTopRightText = (TextView) this.findViewById(R.id.topRight);
        mPreViewLayout = (FrameLayout) this.findViewById(R.id.previewLayout);
        mScanLineImage = (ImageView) this.findViewById(R.id.scan_line);
        mScanContentText = (TextView) this.findViewById(R.id.scanContent);
        mContinueScanText = (TextView) this.findViewById(R.id.continueScan);

        mTranslateAnimation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f);
        mTranslateAnimation.setDuration(2000);
        mTranslateAnimation.setRepeatCount(-1);
        mTranslateAnimation.setRepeatMode(Animation.RESTART);
    }

    private void regListener() {
        mTopRightText.setOnClickListener(new OnClickListenerImp());
        mContinueScanText.setOnClickListener(new OnClickListenerImp());
    }

    private void loadData() {
        // 自动聚焦线程
        mAutoFocusHandler = new Handler();
        // 实例化Scanner
        mScanner = new ImageScanner();
        mScanner.setConfig(0, Config.X_DENSITY, 3);
        mScanner.setConfig(0, Config.Y_DENSITY, 3);
    }

    // 扫描动画
    private void startAnimation(View view) {
        if (view == null)
            return;
        view.startAnimation(mTranslateAnimation);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initCamera();
        if (mCamera == null) {
            // 在这里写下获取相机失败的代码
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
            mBuilder.setTitle("ZbarTest");
            mBuilder.setMessage("ZBarTest获取相机失败，请重试！");
            mBuilder.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface mDialogInterface,
                                            int mIndex) {
                            ZxingActivity.this.finish();
                        }
                    });
            AlertDialog mDialog = mBuilder.create();
            mDialog.show();
        }
        // 设置相机预览视图
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        mPreViewLayout.addView(mPreview);
        if(!isScanned)
            startAnimation(mScanLineImage);
    }

    // 实现Pause方法
    public void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreViewLayout.removeAllViews();
            mPreview = null;
        }
    }

    @Override
    protected void onDestroy() {
        releaseCamera();
        if (mScanner != null) {
            mScanner.destroy();
            mScanner = null;
        }
        mAutoFocusHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void initCamera() {
        // 获取相机实例
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (isPreview)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            // 获取扫描图片的大小
            Camera.Size mSize = parameters.getPreviewSize();
            // 构造存储图片的Image
            Image mResult = new Image(mSize.width, mSize.height, "Y800");// 第三个参数不知道是干嘛的
            // 设置Image的数据资源
            mResult.setData(data);
            // 获取扫描结果的代码
            int mResultCode = mScanner.scanImage(mResult);
            // 如果代码不为0，表示扫描成功
            if (mResultCode != 0) {
                // 停止扫描
                stopPreview();
                // 开始解析扫描图片
                SymbolSet Syms = mScanner.getResults();
                for (Symbol mSym : Syms) {
                    // mSym.getType()方法可以获取扫描的类型，ZBar支持多种扫描类型,这里实现了条形码、二维码、ISBN码的识别
                    int type = mSym.getType();
                    if (type == Symbol.CODE128
                            || type == Symbol.QRCODE
                            || type == Symbol.CODABAR
                            || type == Symbol.ISBN10
                            || type == Symbol.ISBN13
                            || type == Symbol.DATABAR
                            || type == Symbol.DATABAR_EXP
                            || type == Symbol.I25
                            || type == Symbol.UPCA
                            || type == Symbol.UPCE
                            || type == Symbol.EAN8
                            || type == Symbol.EAN13) {
                        // 添加震动效果，提示用户扫描完成
                        Vibrator mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        mVibrator.vibrate(400);
                        String result = "扫描类型:" + GetResultByCode(mSym.getType()) + "\n" + mSym.getData();
                        StringHelper stringHelper = new StringHelper(result);
                        result = stringHelper.SplitFormDict();
                        mScanContentText.append("\n" + result);
                        // 这里需要注意的是，getData方法才是最终返回识别结果的方法
                        // 但是这个方法是返回一个标识型的字符串，换言之，返回的值中包含每个字符串的含义
                        // 例如N代表姓名，URL代表一个Web地址等等，其它的暂时不清楚，如果可以对这个进行一个较好的分割
                        // 效果会更好，如果需要返回扫描的图片，可以对Image做一个合适的处理
                    } else {
                        // 否则继续扫描
                        startPreview();
                    }
                }
            }  else {
                // 否则继续扫描
                startPreview();
            }
        }
    };

    /**
     * 开始扫描
     */
    protected void startPreview() {
        isScanned = false;
        isPreview = true;
        if (mCamera != null) {
            mCamera.setPreviewCallback(previewCb);
            mCamera.startPreview();
            mCamera.autoFocus(autoFocusCB);
        } else {
            Log.i("TAG", "mCamera is null, not startPreview.");
        }
        startAnimation(mScanLineImage);
    }

    /**
     * 停止抓取数据
     */
    protected void stopPreview() {
        isScanned = true;
        isPreview = false;
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        } else {
            Log.i("TAG", "mCamera is null. stopPreview.");
        }
        if (mTranslateAnimation != null)
            mTranslateAnimation.cancel();
    }

    // 释放照相机
    private void releaseCamera() {
        if (mCamera != null) {
            isPreview = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    // 用于刷新自动聚焦的方法
    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            mAutoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    // 根据返回的代码值来返回相应的格式化数据
    public String GetResultByCode(int CodeType) {
        String mResult = "";
        switch (CodeType) {
            // 条形码
            case Symbol.CODABAR:
                mResult = "条形码";
                break;
            // 128编码格式二维码)
            case Symbol.CODE128:
                mResult = "二维码";
                break;
            // QR码二维码
            case Symbol.QRCODE:
                mResult = "二维码";
                break;
            // ISBN10图书查询
            case Symbol.ISBN10:
                mResult = "图书ISBN号";
                break;
            // ISBN13图书查询
            case Symbol.ISBN13:
                mResult = "图书ISBN号";
                break;
        }
        return mResult;
    }

    class OnClickListenerImp implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.topRight:
                    Intent innerIntent = new Intent(); // "android.intent.action.GET_CONTENT"
//                    if (Build.VERSION.SDK_INT < 19) {
//                        innerIntent.setAction(Intent.ACTION_GET_CONTENT);
//                    } else {
//                        innerIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
//                    }
//                    innerIntent.setAction(Intent.ACTION_GET_CONTENT);
//                    innerIntent.setType("image/*");
//                    Intent wrapperIntent = Intent.createChooser(innerIntent, "选择二维码图片");
//                    ZxingActivity.this.startActivityForResult(wrapperIntent, REQUEST_CODE);

                    if (mPreview != null) {
                        mPreViewLayout.removeAllViews();
                        mPreview = null;
                    }
                    break;
                case R.id.continueScan:
                    if (isScanned) {
                        isScanned = false;
                        isPreview = true;
                        startPreview();
                    }
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE:
                    // 从本地取出照片
                    String[] proj = {MediaStore.Images.Media.DATA};
                    // 获取选中图片的路径
                    String photoPath = null;
                    Cursor cursor = getContentResolver().query(data.getData(), proj, null, null, null);
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        photoPath = cursor.getString(columnIndex);
                        if (photoPath == null) {
                            photoPath = ScanUtils.getPath(getApplicationContext(), data.getData());
                        }
                    }
                    cursor.close();
                    Bitmap bitmap = BitmapUtils.getCompressedBitmap(photoPath);
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int[] pixels = new int[width * height];
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                    Image image = new Image(width, height, "RGB4");
                    image.setData(pixels);
                    int result = mScanner.scanImage(image.convert("Y800"));
                    // 如果代码不为0，表示扫描成功
                    if (result != 0) {
                        // 停止扫描
                        stopPreview();
                        // 开始解析扫描图片
                        SymbolSet Syms = mScanner.getResults();
                        for (Symbol mSym : Syms) {
                            // mSym.getType()方法可以获取扫描的类型，ZBar支持多种扫描类型,这里实现了条形码、二维码、ISBN码的识别
                            int type = mSym.getType();
                            if (type == Symbol.CODE128
                                    || type == Symbol.QRCODE
                                    || type == Symbol.CODABAR
                                    || type == Symbol.ISBN10
                                    || type == Symbol.ISBN13
                                    || type == Symbol.DATABAR
                                    || type == Symbol.DATABAR_EXP
                                    || type == Symbol.I25
                                    || type == Symbol.UPCA
                                    || type == Symbol.UPCE
                                    || type == Symbol.EAN8
                                    || type == Symbol.EAN13) {
                                // 添加震动效果，提示用户扫描完成
                                Vibrator mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                                mVibrator.vibrate(400);
                                String resultContent = "扫描类型:" + GetResultByCode(mSym.getType()) + "\n" + mSym.getData();
                                StringHelper stringHelper = new StringHelper(resultContent);
                                resultContent = stringHelper.SplitFormDict();
                                Toast.makeText(ZxingActivity.this, resultContent, Toast.LENGTH_LONG).show();
                                mScanContentText.append("\n" + resultContent);
                                // 这里需要注意的是，getData方法才是最终返回识别结果的方法
                                // 但是这个方法是返回一个标识型的字符串，换言之，返回的值中包含每个字符串的含义
                                // 例如N代表姓名，URL代表一个Web地址等等，其它的暂时不清楚，如果可以对这个进行一个较好的分割
                                // 效果会更好，如果需要返回扫描的图片，可以对Image做一个合适的处理
                            }
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ZxingActivity.this, "图片格式有误", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    break;
            }
        }
    }
}
