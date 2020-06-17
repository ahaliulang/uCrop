package com.yalantis.ucrop.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

import com.yalantis.ucrop.util.RectUtils;

/**
 * author:tdn
 * time:2020/6/17
 * description:
 */
public class OverlayViewExtension extends OverlayView {


    public OverlayViewExtension(Context context) {
        this(context, null);
    }

    public OverlayViewExtension(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayViewExtension(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }


    @Override
    protected void init() {
        mFrameValueAnimator = ValueAnimator.ofFloat(0, 1f);
        mFrameValueAnimator.setInterpolator(new DecelerateInterpolator());
    }

    @Override
    protected void drawCropGrid(@NonNull Canvas canvas) {
        super.drawCropGrid(canvas);
        if (mShowCropGrid) {
            if (mGridPoints == null && !mCropViewRect.isEmpty()) {

                mGridPoints = new float[(mCropGridRowCount) * 4 + (mCropGridColumnCount) * 4];

                int index = 0;
                for (int i = 0; i < mCropGridRowCount; i++) {
                    mGridPoints[index++] = mCropViewRect.left;
                    mGridPoints[index++] = (mCropViewRect.height() * (((float) i + 1.0f) / (float) (mCropGridRowCount + 1))) + mCropViewRect.top;
                    mGridPoints[index++] = mCropViewRect.right;
                    mGridPoints[index++] = (mCropViewRect.height() * (((float) i + 1.0f) / (float) (mCropGridRowCount + 1))) + mCropViewRect.top;
                }

                for (int i = 0; i < mCropGridColumnCount; i++) {
                    mGridPoints[index++] = (mCropViewRect.width() * (((float) i + 1.0f) / (float) (mCropGridColumnCount + 1))) + mCropViewRect.left;
                    mGridPoints[index++] = mCropViewRect.top;
                    mGridPoints[index++] = (mCropViewRect.width() * (((float) i + 1.0f) / (float) (mCropGridColumnCount + 1))) + mCropViewRect.left;
                    mGridPoints[index++] = mCropViewRect.bottom;
                }
            }

            if (mGridPoints != null) {
                canvas.drawLines(mGridPoints, mCropGridPaint);
            }
        }

        if (mShowCropFrame) {
            canvas.drawRect(mCropViewRect, mCropFramePaint);
        }

        // TODO:tdn 2020/6/17
//        if (mFreestyleCropMode != FREESTYLE_CROP_MODE_DISABLE) {
//            canvas.save();
//
//            mTempRect.set(mCropViewRect);
//            mTempRect.inset(mCropRectCornerTouchAreaLineLength, -mCropRectCornerTouchAreaLineLength);
//            canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);
//
//            mTempRect.set(mCropViewRect);
//            mTempRect.inset(-mCropRectCornerTouchAreaLineLength, mCropRectCornerTouchAreaLineLength);
//            canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);
//
//            canvas.drawRect(mCropViewRect, mCropFrameCornersPaint);
//
//            canvas.restore();
//        }

        drawHandleLines(canvas);
    }

    /**
     * 画四角的线
     *
     * @param canvas
     */
    private void drawHandleLines(Canvas canvas) {

        canvas.drawCircle(mCropViewRect.left, mCropViewRect.top, 30, mCropFrameCornersPaint);
        canvas.drawCircle(mCropViewRect.right, mCropViewRect.top, 30, mCropFrameCornersPaint);
        canvas.drawCircle(mCropViewRect.right, mCropViewRect.bottom, 30, mCropFrameCornersPaint);
        canvas.drawCircle(mCropViewRect.left, mCropViewRect.bottom, 30, mCropFrameCornersPaint);

        float centerX = mCropGridCenter[0];
        float centerY = mCropGridCenter[1];
        float mHandleSize = 40;

        // TODO:tdn 2020/6/17 圆角
        canvas.drawLine(centerX - mHandleSize, mCropViewRect.top, (centerX + mHandleSize), mCropViewRect.top,
                mCropFrameCornersPaint);
        canvas.drawLine(centerX - mHandleSize, mCropViewRect.bottom, centerX + mHandleSize, mCropViewRect.bottom,
                mCropFrameCornersPaint);
        canvas.drawLine(mCropViewRect.left, (centerY - mHandleSize), mCropViewRect.left, centerY + mHandleSize,
                mCropFrameCornersPaint);
        canvas.drawLine(mCropViewRect.right, centerY - mHandleSize, mCropViewRect.right, centerY + mHandleSize,
                mCropFrameCornersPaint);
    }

    @Override
    protected void updateGridPoints() {
        mCropGridCorners = RectUtils.getCornersFromRectFor8(mCropViewRect);
        mCropGridCenter = RectUtils.getCenterFromRect(mCropViewRect);

        mGridPoints = null;
        mCircularPath.reset();
        mCircularPath.addCircle(mCropViewRect.centerX(), mCropViewRect.centerY(),
                Math.min(mCropViewRect.width(), mCropViewRect.height()) / 2.f, Path.Direction.CW);
    }

    protected int getCurrentTouchIndex(float touchX, float touchY) {
        int closestPointIndex = -1;
        double closestPointDistance = mTouchPointThreshold;
        for (int i = 0; i < 16; i += 2) {
            double distanceToCorner = Math.sqrt(Math.pow(touchX - mCropGridCorners[i], 2)
                    + Math.pow(touchY - mCropGridCorners[i + 1], 2));
            if (distanceToCorner < closestPointDistance) {
                closestPointDistance = distanceToCorner;
                closestPointIndex = i / 2;
            }
        }

        if (mFreestyleCropMode == FREESTYLE_CROP_MODE_ENABLE && closestPointIndex < 0 && mCropViewRect.contains(touchX, touchY)) {
            return 8;
        }
        return closestPointIndex;
    }


    private void updateCropViewRect(float touchX, float touchY, float diffX, float diffY) {
        mTempRect.set(mCropViewRect);
        switch (mCurrentTouchCornerIndex) {
            // resize rectangle
            case 0:
                if (diffX > diffY) {
                    diffY = diffX * getRatioY() / getRatioX();
                } else {
                    diffX = diffY * getRatioX() / getRatioY();
                }
                mTempRect.set(mCropViewRect.left + diffX, mCropViewRect.top + diffY, mCropViewRect.right, mCropViewRect.bottom);
                break;
            case 1:
                mTempRect.set(mCropViewRect.left, touchY, mCropViewRect.right, mCropViewRect.bottom);
                break;
            case 2:
                mTempRect.set(mCropViewRect.left, touchY, touchX, mCropViewRect.bottom);
                break;
            case 3:
                mTempRect.set(mCropViewRect.left, mCropViewRect.top, touchX, mCropViewRect.bottom);
                break;
            case 4:
                mTempRect.set(mCropViewRect.left, mCropViewRect.top, touchX, touchY);
                break;
            case 5:
                mTempRect.set(mCropViewRect.left, mCropViewRect.top, mCropViewRect.right, touchY);
                break;
            case 6:
                mTempRect.set(touchX, mCropViewRect.top, mCropViewRect.right, touchY);
                break;
            case 7:
                mTempRect.set(touchX, mCropViewRect.top, mCropViewRect.right, mCropViewRect.bottom);
                break;
            // move rectangle
            case 8:
                mTempRect.offset(touchX - mPreviousTouchX, touchY - mPreviousTouchY);
                if (mTempRect.left > getLeft() && mTempRect.top > getTop()
                        && mTempRect.right < getRight() && mTempRect.bottom < getBottom()) {
                    mCropViewRect.set(mTempRect);
                    updateGridPoints();
                    postInvalidate();
                }
                return;
        }

        boolean changeHeight = mTempRect.height() >= mCropRectMinSize;
        boolean changeWidth = mTempRect.width() >= mCropRectMinSize;
        if (getRatioY() > getRatioX()) {
            changeHeight = mTempRect.height() >= mCropRectMinSize * (getRatioY() / getRatioX());
        } else {
            changeWidth = mTempRect.width() >= mCropRectMinSize * (getRatioX() / getRatioY());
        }
        mCropViewRect.set(
                changeWidth ? mTempRect.left : mCropViewRect.left,
                changeHeight ? mTempRect.top : mCropViewRect.top,
                changeWidth ? mTempRect.right : mCropViewRect.right,
                changeHeight ? mTempRect.bottom : mCropViewRect.bottom);

        if (changeHeight || changeWidth) {
            updateGridPoints();
            postInvalidate();
        }
    }

    private float getRatioX() {
        return 2f;
    }

    private float getRatioY() {
        return 3f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCropViewRect.isEmpty() || mFreestyleCropMode == FREESTYLE_CROP_MODE_DISABLE) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            mCurrentTouchCornerIndex = getCurrentTouchIndex(x, y);
            boolean shouldHandle = mCurrentTouchCornerIndex != -1;
            if (!shouldHandle) {
                mPreviousTouchX = -1;
                mPreviousTouchY = -1;
            } else if (mPreviousTouchX < 0) {
                mPreviousTouchX = x;
                mPreviousTouchY = y;
            }
            return shouldHandle;
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() == 1 && mCurrentTouchCornerIndex != -1) {

                x = Math.min(Math.max(x, getPaddingLeft()), getWidth() - getPaddingRight());
                y = Math.min(Math.max(y, getPaddingTop()), getHeight() - getPaddingBottom());

                float diffX = x - mPreviousTouchX;
                float diffY = y - mPreviousTouchY;

                updateCropViewRect(x, y, diffX, diffY);

                mPreviousTouchX = x;
                mPreviousTouchY = y;

                return true;
            }
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            mPreviousTouchX = -1;
            mPreviousTouchY = -1;
            mCurrentTouchCornerIndex = -1;

            if (mCallback != null) {
                mCallback.onCropRectUpdated(mCropViewRect);
            }

            recalculateFrameRect(3000);
        }
        return false;
    }


    private ValueAnimator mFrameValueAnimator = null;

    private void recalculateFrameRect(int durationMillis) {

        final RectF newRect = new RectF(200, 200, 800, 800);

        if (mFrameValueAnimator.isRunning()) {
            mFrameValueAnimator.cancel();
        }

        final RectF currentRect = new RectF(mCropViewRect);
        final float diffL = newRect.left - currentRect.left;
        final float diffT = newRect.top - currentRect.top;
        final float diffR = newRect.right - currentRect.right;
        final float diffB = newRect.bottom - currentRect.bottom;
        mFrameValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mCropViewRect.set(newRect);
                updateGridPoints();
                postInvalidate();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
            }
        });
        mFrameValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float scale = (float) animation.getAnimatedValue();
                // 裁剪框放大的动画RectF
                mCropViewRect.set(currentRect.left + diffL * scale, currentRect.top + diffT * scale, currentRect.right
                        + diffR * scale, currentRect.bottom + diffB * scale);
                updateGridPoints();
                postInvalidate();
            }
        });
        mFrameValueAnimator.setDuration(durationMillis);
        mFrameValueAnimator.start();
    }


}
