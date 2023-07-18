package cu.axel.smartdock.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import cu.axel.smartdock.R;
import android.graphics.Color;
import android.util.AttributeSet;
import android.animation.ValueAnimator;

public class VisualizerView extends View {

	private byte[] audioData;
	private Paint barPaint;
	private float barSpacing;
	//TODO: This is a hack
	private float barHeightScalingFactor;
	private int barNumber = 120;
	private float smoothingFactor = 0.4f;
	private int[] smoothedAmplitudes;
	private int[] targetAmplitudes;
	private ValueAnimator[] animators;

	public VisualizerView(Context context) {
		super(context);
		init();
	}

	public VisualizerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		barPaint = new Paint();
		barPaint.setColor(Color.WHITE);
		barSpacing = 3f;
		barHeightScalingFactor = 1.01f;

		smoothedAmplitudes = new int[barNumber];
		targetAmplitudes = new int[barNumber];
		animators = new ValueAnimator[barNumber];
	}

	public void setAudioData(byte[] data) {
		audioData = data;
		smoothAmplitudes();
		startAnimations();
		invalidate();
	}

	private void smoothAmplitudes() {
		if (smoothedAmplitudes.length != barNumber) {
			smoothedAmplitudes = new int[barNumber];
		}

		for (int i = 0; i < barNumber; i++) {
			int startIndex = i * (audioData.length / barNumber);
			int endIndex = (i + 1) * (audioData.length / barNumber);
			int maxAmplitude = getMaxAmplitudeInRange(startIndex, endIndex);

			targetAmplitudes[i] = (int) (maxAmplitude * (1.0f - smoothingFactor)
					+ smoothedAmplitudes[i] * smoothingFactor);
		}
	}

	private void startAnimations() {
		for (int i = 0; i < barNumber; i++) {
			final int index = i;

			animators[i] = ValueAnimator.ofInt(smoothedAmplitudes[i], targetAmplitudes[i]);
			animators[i].setDuration(50);
			animators[i].addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animator) {
					smoothedAmplitudes[index] = (int) animator.getAnimatedValue();
					invalidate();
				}
			});
			animators[i].start();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (audioData == null) {
			return;
		}

		int width = getWidth();
		int height = getHeight();
		int totalBarWidth = width / barNumber;
		//barSpacing = totalBarWidth * 0.3f;
		float barWidth = totalBarWidth - barSpacing;

		for (int i = 0; i < barNumber; i++) {
			float barHeight = height - (smoothedAmplitudes[i] * height * barHeightScalingFactor / 128f);
			float left = i * totalBarWidth + barSpacing;
			float top = height - barHeight;
			float right = left + barWidth;
			float bottom = height;

			canvas.drawRect(left, top, right, bottom, barPaint);
		}
	}

	private int getMaxAmplitudeInRange(int start, int end) {
		int maxAmplitude = 0;
		for (int i = start; i < end; i++) {
			maxAmplitude = Math.max(maxAmplitude, Math.abs(audioData[i]));
		}
		return maxAmplitude;
	}

	public void setSmoothingFactor(float smoothing) {
		smoothingFactor = smoothing;
	}

	public void setBarNumber(int number) {
		barNumber = number;
		init();
	}
	
	public void setBarColor(int color) {
		barPaint.setColor(color);
	}
	
	public void setBarAlpha(int alpha) {
		barPaint.setAlpha(alpha);
	}
}
