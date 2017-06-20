package javax.microedition.m3g;

import java.lang.Integer;
import java.lang.Float;

public class AnimationTrack extends Object3D {

	public static final int ALPHA = 256;
	public static final int AMBIENT_COLOR = 257;
	public static final int COLOR = 258;
	public static final int CROP = 259;
	public static final int DENSITY = 260;
	public static final int DIFFUSE_COLOR = 261;
	public static final int EMISSIVE_COLOR = 262;
	public static final int FAR_DISTANCE = 263;
	public static final int FIELD_OF_VIEW = 264;
	public static final int INTENSITY = 265;
	public static final int MORPH_WEIGHTS = 266;
	public static final int NEAR_DISTANCE = 267;
	public static final int ORIENTATION = 268;
	public static final int PICKABILITY = 269;
	public static final int SCALE = 270;
	public static final int SHININESS = 271;
	public static final int SPECULAR_COLOR = 272;
	public static final int SPOT_ANGLE = 273;
	public static final int SPOT_EXPONENT = 274;
	public static final int TRANSLATION = 275;
	public static final int VISIBILITY = 276;

	KeyframeSequence sequence;
	int property;
	private AnimationController controller;

	public AnimationTrack(KeyframeSequence sequence, int property) {
		if (sequence == null) {
			throw new NullPointerException("Sequence must not be null");
		}
		if ((property < ALPHA) || (property > VISIBILITY)) {
			throw new IllegalArgumentException("Unknown property");
		}
		if (!isCompatible(sequence.getComponentCount(), property)) {
			throw new IllegalArgumentException("Sequence is not compatible with property");
		}
		this.sequence = sequence;
		this.property = property;
	}

	Object3D duplicateImpl() {
		AnimationTrack copy = new AnimationTrack(sequence, property);
		copy.controller = controller;
		return copy;
	}

	@Override
	int doGetReferences(Object3D[] references) {
		int num = super.doGetReferences(references);
		if (sequence != null) {
			if (references != null)
				references[num] = (Object3D) sequence;
			num++;
		}
		if (controller != null) {
			if (references != null)
				references[num] = (Object3D) controller;
			num++;
		}
		return num;
	}

	@Override
	Object3D findID(int userID) {
		Object3D found = super.findID(userID);

		if (found == null && sequence != null)
			found = sequence.findID(userID);
		if (found == null && controller != null)
			found = controller.findID(userID);

		return found;
	}

	void getContribution(int time, float[] accumSamples, float[] weight, int[] validity) {
		if (this.controller == null || !controller.isActive(time)) {
			weight[0] = 0;
			validity[0] = (controller != null ? controller.timeToActivation(time) : 0x7FFFFFFF);
			if (validity[0] < 1)
				validity[0] = 1;
			return;
		}

		int sampleLength = sequence.getComponentCount();
		weight[0] = controller.getWeight();

		if (weight[0] <= 0.0f) {
			validity[0] = 0x7FFFFFFF;
			return;
		}

		float[] sample = new float[sampleLength];

		int sampleTime = (int) controller.getPosition(time);
		int sampleValidity = sequence.getSample(sampleTime, sample);

		if (sampleValidity > 0) {
			sampleValidity = controller.timeToDeactivation(time);
			if (sampleValidity < validity[0])
				validity[0] = sampleValidity;

			for (int i = 0; i < sampleLength; i++)
				accumSamples[i] += sample[i] * weight[0];
		}
	}

	public AnimationController getController() {
		return controller;
	}

	public void setController(AnimationController controller) {
		this.controller = controller;
	}

	public int getTargetProperty() {
		return property;
	}

	public KeyframeSequence getKeyframeSequence() {
		return sequence;
	}

	private boolean isCompatible(int components, int property) {
		switch (property) {
		case ALPHA:
		case DENSITY:
		case FAR_DISTANCE:
		case FIELD_OF_VIEW:
		case INTENSITY:
		case NEAR_DISTANCE:
		case PICKABILITY:
		case SHININESS:
		case SPOT_ANGLE:
		case SPOT_EXPONENT:
		case VISIBILITY:
			return components == 1;
		case CROP:
			return components == 2 || components == 4;
		case AMBIENT_COLOR:
		case COLOR:
		case DIFFUSE_COLOR:
		case EMISSIVE_COLOR:
		case SPECULAR_COLOR:
		case TRANSLATION:
			return components == 3;
		case SCALE:
			return components == 1 || components == 3;
		case ORIENTATION:
			return components == 4;
		case MORPH_WEIGHTS:
			return components > 0;
		default:
			return false; // Shouldn't occur
		}
	}

}
