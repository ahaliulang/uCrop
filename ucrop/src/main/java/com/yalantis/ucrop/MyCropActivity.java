package com.yalantis.ucrop;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.yalantis.ucrop.callback.BitmapCropCallback;
import com.yalantis.ucrop.model.AspectRatio;
import com.yalantis.ucrop.view.CropImageView;
import com.yalantis.ucrop.view.GestureCropImageView;
import com.yalantis.ucrop.view.OverlayView;
import com.yalantis.ucrop.view.TransformImageView;
import com.yalantis.ucrop.view.UCropView;
import com.yalantis.ucrop.view.widget.AspectRatioTextView;

import java.util.ArrayList;
import java.util.List;

public class MyCropActivity extends AppCompatActivity {

    private static final String TAG = "MyCropActivity";

    private UCropView mUCropView;
    private GestureCropImageView mGestureCropImageView;
    private OverlayView mOverlayView;

    private List<View> mCropAspectRatioViews = new ArrayList<>();


    private TransformImageView.TransformImageListener mImageListener = new TransformImageView.TransformImageListener() {
        @Override
        public void onRotate(float currentAngle) {
            Log.d(TAG, "onRotate: " + currentAngle);
        }

        @Override
        public void onScale(float currentScale) {
            Log.d(TAG, "onScale: " + currentScale);
        }

        @Override
        public void onLoadComplete() {
            mUCropView.animate().alpha(1).setDuration(300).setInterpolator(new AccelerateInterpolator());
        }

        @Override
        public void onLoadFailure(@NonNull Exception e) {
            finish();
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ucrop_activity_my_crop);

        mUCropView = findViewById(R.id.ucrop);
        mGestureCropImageView = mUCropView.getCropImageView();
        mGestureCropImageView.setRotateEnabled(false);
        mOverlayView = mUCropView.getOverlayView();
        mOverlayView.setFreestyleCropEnabled(true);
        mGestureCropImageView.setTransformImageListener(mImageListener);

        setImageData(getIntent());

        setupAspectRatioWidget();
    }


    /**
     * This method extracts all data from the incoming intent and setups views properly.
     */
    private void setImageData(@NonNull Intent intent) {
        Uri inputUri = intent.getParcelableExtra(UCrop.EXTRA_INPUT_URI);
        Uri outputUri = intent.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI);

        if (inputUri != null && outputUri != null) {
            try {
                mGestureCropImageView.setImageUri(inputUri, outputUri);
            } catch (Exception e) {
                setResultError(e);
                finish();
            }
        } else {
            setResultError(new NullPointerException(getString(R.string.ucrop_error_input_data_is_absent)));
            finish();
        }
    }

    private void setupAspectRatioWidget() {

        List<AspectRatio> aspectRatioList = new ArrayList<>();

        if (aspectRatioList == null || aspectRatioList.isEmpty()) {
            aspectRatioList = new ArrayList<>();
            aspectRatioList.add(new AspectRatio("Free",
                    CropImageView.SOURCE_IMAGE_ASPECT_RATIO, CropImageView.SOURCE_IMAGE_ASPECT_RATIO));
            aspectRatioList.add(new AspectRatio(null, 1, 1));
            aspectRatioList.add(new AspectRatio(null, 4, 5));
            aspectRatioList.add(new AspectRatio(null, 3, 4));
            aspectRatioList.add(new AspectRatio(null, 4, 3));
            aspectRatioList.add(new AspectRatio(null, 16, 9));
            aspectRatioList.add(new AspectRatio(null, 9, 16));
        }

        LinearLayout wrapperAspectRatioList = findViewById(R.id.layout_aspect_ratio);

        AspectRatioTextView aspectRatioTextView;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        for (AspectRatio aspectRatio : aspectRatioList) {

            aspectRatioTextView = new AspectRatioTextView(this);
            aspectRatioTextView.setPadding(90,0,90,0);
            aspectRatioTextView.setActiveColor(Color.parseColor("#ff2a9b"));
            aspectRatioTextView.setAspectRatio(aspectRatio);
            aspectRatioTextView.setLayoutParams(lp);
            wrapperAspectRatioList.addView(aspectRatioTextView);

            mCropAspectRatioViews.add(aspectRatioTextView);
        }

        mCropAspectRatioViews.get(0).setSelected(true);
        for (int i = 0; i < mCropAspectRatioViews.size(); i++) {
            View cropAspectRatioView = mCropAspectRatioViews.get(i);
            final int finalI = i;
            cropAspectRatioView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(finalI == 0){
                        mOverlayView.setFreestyleCropEnabled(true);
                    }else {
                        mOverlayView.setFreestyleCropEnabled(false);
                    }

                    mGestureCropImageView.setTargetAspectRatio(((AspectRatioTextView) v).getAspectRatio(false));
                    mGestureCropImageView.setImageToWrapCropBounds();
                    if (!v.isSelected()) {
                        for (View cropAspectRatioView : mCropAspectRatioViews) {
                            cropAspectRatioView.setSelected(cropAspectRatioView == v);
                        }
                    }
                }
            });
        }
    }

    protected void setResultUri(Uri uri, float resultAspectRatio, int offsetX, int offsetY, int imageWidth, int imageHeight) {
        setResult(RESULT_OK, new Intent()
                .putExtra(UCrop.EXTRA_OUTPUT_URI, uri)
                .putExtra(UCrop.EXTRA_OUTPUT_CROP_ASPECT_RATIO, resultAspectRatio)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_X, offsetX)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_Y, offsetY)
        );
    }


    protected void cropAndSaveImage() {

        mGestureCropImageView.cropAndSaveImage(Bitmap.CompressFormat.JPEG, 90, new BitmapCropCallback() {

            @Override
            public void onBitmapCropped(@NonNull Uri resultUri, int offsetX, int offsetY, int imageWidth, int imageHeight) {
                setResultUri(resultUri, mGestureCropImageView.getTargetAspectRatio(), offsetX, offsetY, imageWidth, imageHeight);
                finish();
            }

            @Override
            public void onCropFailure(@NonNull Throwable t) {
                setResultError(t);
                finish();
            }
        });
    }

    protected void setResultError(Throwable throwable) {
        setResult(UCrop.RESULT_ERROR, new Intent().putExtra(UCrop.EXTRA_ERROR, throwable));
    }


}
