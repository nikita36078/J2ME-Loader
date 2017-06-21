package javax.microedition.m3g;

public class KeyframeSequence extends Object3D {

	public static final int CONSTANT = 192;
	public static final int LINEAR = 176;
	public static final int LOOP = 193;
	public static final int SLERP = 177;
	public static final int SPLINE = 178;
	public static final int SQUAD = 179;
	public static final int STEP = 180;

	private int repeatMode = CONSTANT;
	private int duration;
	private int validRangeFirst;
	private int validRangeLast;
	private int interpolationType;
	private int keyframeCount;
	int componentCount;
	private int probablyNext;
	private boolean dirty;

	private float[][] keyFrames;
	private float[][] inTangents;
	private float[][] outTangents;
	private int[] keyFrameTimes;
	private QVec4[] a;
	private QVec4[] b;
	
	private KeyframeSequence() {
		//dirty = true;
	}

	public KeyframeSequence(int numKeyframes, int numComponents, int interpolation) {

		if ((numKeyframes < 1) || (numComponents < 1)) {
			throw new IllegalArgumentException("Number of keyframes/components must be >= 1");
		}

		// Check given interpolation mode
		switch (interpolation) {
		case SLERP:
			if (numComponents != 4)
				throw new IllegalArgumentException("SLERP and SQUAD mode requires 4 components in each keyframe");
			break;
		case SQUAD:
			if (numComponents != 4)
				throw new IllegalArgumentException("SLERP and SQUAD mode requires 4 components in each keyframe");
			a = new QVec4[numKeyframes];
			b = new QVec4[numKeyframes];
			break;
		case STEP:
			break;
		case LINEAR:
			break;
		case SPLINE:
			inTangents = new float[numKeyframes][numComponents];
			outTangents = new float[numKeyframes][numComponents];
			break;
		default:
			throw new IllegalArgumentException("Unknown interpolation mode");
		}

		this.keyframeCount = numKeyframes;
		this.componentCount = numComponents;
		this.interpolationType = interpolation;

		// Initialize the sequence with default values
		keyFrames = new float[numKeyframes][numComponents];
		keyFrameTimes = new int[numKeyframes];
		validRangeFirst = 0;
		validRangeLast = numKeyframes - 1;
		dirty = true;


	}
	
	Object3D duplicateImpl() {
		KeyframeSequence copy = new KeyframeSequence(keyframeCount, componentCount, interpolationType);
		copy.repeatMode = repeatMode;
		copy.duration = duration;
		copy.validRangeFirst = validRangeFirst;
		copy.validRangeLast = validRangeLast;
		//copy.interpolationType = interpolationType;
		//copy.keyframeCount = keyframeCount;
		//copy.componentCount = componentCount;
		//copy.probablyNext = probablyNext;
		
		copy.keyFrames = new float[keyFrames.length][keyFrames[0].length];
		for (int i = 0; i < keyFrames.length; i++)
			System.arraycopy(keyFrames[i], 0, copy.keyFrames[i], 0, keyFrames[i].length);
		copy.keyFrameTimes = new int[keyFrameTimes.length];
		System.arraycopy(keyFrameTimes, 0, copy.keyFrameTimes, 0, keyFrameTimes.length);

		if (!dirty) {
			copy.dirty = false;
			if (inTangents != null) {
				copy.inTangents = new float[inTangents.length][inTangents[0].length];
				for (int i = 0; i < inTangents.length; i++)
					System.arraycopy(inTangents[i], 0, copy.inTangents[i], 0, inTangents[i].length);
				copy.outTangents = new float[outTangents.length][outTangents[0].length];
				for (int i = 0; i < outTangents.length; i++)
					System.arraycopy(outTangents[i], 0, copy.outTangents[i], 0, inTangents[i].length);
			}
			if (a != null) {
				copy.a = new QVec4[a.length];
				for (int i = 0; i < a.length; i++)
					copy.a[i].assign(a[i]);
				copy.b = new QVec4[b.length];
				for (int i = 0; i < b.length; i++)
					copy.b[i].assign(b[i]);
			}
		} else
			copy.dirty = true;
		return copy;
	}

	float[] keyframeAt(int idx) {
		return keyFrames[idx];
	}

	int previousKeyframeIndex(int ind) {
		if (ind == validRangeFirst)
			return validRangeLast;
		else if (ind == 0)
			return keyframeCount - 1;
		else
			return ind - 1;
	}

	int nextKeyframeIndex(int ind) {
		if (ind == validRangeLast)
			return validRangeFirst;
		else if (ind == (keyframeCount - 1))
			return 0;
		else
			return (ind + 1);
	}

	float[] keyframeBefore(int idx) {
		return keyframeAt(previousKeyframeIndex(idx));
	}

	float[] keyframeAfter(int idx) {
		return keyframeAt(nextKeyframeIndex(idx));
	}

	int timeDelta(int ind) {
		if (ind == validRangeLast)
			return (duration - keyFrameTimes[validRangeLast]) + keyFrameTimes[validRangeFirst];

		return keyFrameTimes[nextKeyframeIndex(ind)] - keyFrameTimes[ind];
	}

	float incomingTangentScale(int ind) {
		if (repeatMode != LOOP && (ind == validRangeFirst || ind == validRangeLast))
			return 0;
		else {
			int prevind = previousKeyframeIndex(ind);
			return (((float) timeDelta(prevind) * 2.0f)/((float)(timeDelta(ind) + timeDelta(prevind))));
		}
	}

	float outgoingTangentScale(int ind) {
		if (repeatMode != LOOP && (ind == validRangeFirst || ind == validRangeLast))
			return 0;
		else {
			int prevind = previousKeyframeIndex(ind);
			return (((float) timeDelta(ind) * 2.0f)/((float)(timeDelta(ind) + timeDelta(prevind))));
		}
	}

	float[] tangentTo(int idx) {
		if (inTangents == null)
			throw new NullPointerException();
		return inTangents[idx];
	}

	float[] tangentFrom(int idx) {
		if (outTangents == null)
			throw new NullPointerException();
		return outTangents[idx];
	}

	int getSample(int time, float[] sample) {
		if (dirty) {
			if (interpolationType == SPLINE) {
				int kf = validRangeFirst;
				do {
					float[] prev = keyframeBefore(kf);
					float[] next = keyframeAfter(kf);
					float sIn = incomingTangentScale(kf);
					float sOut = outgoingTangentScale(kf);
					float[] in = tangentTo(kf);
					float[] out = tangentFrom(kf);

					for (int i = 0; i < componentCount; i++) {
						in[i]  = ((0.5f * ((next[i] - prev[i]))) * sIn);
						out[i] = ((0.5f * ((next[i] - prev[i]))) * sOut);
					}

					kf = nextKeyframeIndex(kf);
				} while (kf != validRangeFirst);
			} else if (interpolationType == SQUAD) {
				int kf = validRangeFirst;
				QVec4 start = new QVec4();
				QVec4 end = new QVec4();
			       	QVec4 prev = new QVec4();
				QVec4 next = new QVec4();
				QVec4 tempq = new QVec4();
				Vector3 tempv = new Vector3();
				Vector3 cfd = new Vector3();
				Vector3 tangent = new Vector3();
				do {
					prev.setQuat(keyframeBefore(kf));
					start.setQuat(keyframeAt(kf));
					end.setQuat(keyframeAfter(kf));
					next.setQuat(keyframeAfter(nextKeyframeIndex(kf)));

					cfd.logDiffQuat(start, end);
					tempv.logDiffQuat(prev, start);
					cfd.addVec3(tempv);
					cfd.scaleVec3(0.5f);

					tangent.assign(cfd);
					tangent.scaleVec3(outgoingTangentScale(kf));

					tempv.logDiffQuat(start, end);
					tangent.subVec3(tempv);
					tangent.scaleVec3(0.5f);
					tempq.x = tempv.x;
					tempq.y = tempv.y;
					tempq.z = tempv.z;
					tempq.expQuat(tangent);
					a[kf].assign(start);
					a[kf].mulQuat(tempq);

					tangent.assign(cfd);
					tangent.scaleVec3(incomingTangentScale(kf));

					tempv.x = tempq.x;
					tempv.y = tempq.y;
					tempv.z = tempq.z;
					tempv.logDiffQuat(prev, start);
					tempv.subVec3(tangent);
					tempv.scaleVec3(0.5f);
					tempq.x = tempv.x;
					tempq.y = tempv.y;
					tempq.z = tempv.z;
					tempq.expQuat(tempv);
					b[kf].assign(start);
					b[kf].mulQuat(tempq);

					kf = nextKeyframeIndex(kf);
				} while (kf != validRangeFirst);
			}
			dirty = false;
			probablyNext = validRangeFirst;
		}

		if (repeatMode == LOOP) {
			if (time < 0)
				time = (time % duration) + duration;
			else
				time = time % duration;

			if (time < keyFrameTimes[validRangeFirst])
				time += duration;
		} else {
			if (time < keyFrameTimes[validRangeFirst]) {
				float[] value = keyframeAt(validRangeFirst);
				for (int i = 0; i < componentCount; i++)
					sample[i] = value[i];
				return (keyFrameTimes[validRangeFirst] - time);
			} else if (time >= keyFrameTimes[validRangeLast]) {
				float[] value = keyframeAt(validRangeLast);
				for (int i = 0; i < componentCount; i++)
					sample[i] = value[i];
				return 0x7FFFFFFF;
			}
		}

		int start = probablyNext;
		if (keyFrameTimes[start] > time)
			start = validRangeFirst;
		while (start != validRangeLast && keyFrameTimes[nextKeyframeIndex(start)] <= time)
			start = nextKeyframeIndex(start);
		probablyNext = start;

		if (time == keyFrameTimes[start] || interpolationType == STEP) {
			float[] value = keyframeAt(start);
			for (int i = 0; i < componentCount; i++)
				sample[i] = value[i];
			return (interpolationType == STEP) ? (timeDelta(start) - (time - keyFrameTimes[start])) : 1;
		}

		float s = ((time - keyFrameTimes[start]) / (float)timeDelta(start));

		int end = nextKeyframeIndex(start);
		float[] Start;
		float[] End;
		float[] temp;
		float[] tStart;
		float[] tEnd;
		float s2;
		float s3;
		QVec4 q0;
		QVec4 q1;
		QVec4 sampl;
		QVec4 temp0;
		QVec4 temp1;
		QVec4 A;
		QVec4 B;
		switch (interpolationType) {
			case LINEAR:
				Start = keyframeAt(start);
				End = keyframeAt(end);
				Vector3.lerp(componentCount, sample, s, Start, End);
				break;
			case SLERP:
				if (componentCount != 4)
					throw new IllegalStateException();
				q0 = new QVec4();
				q1 = new QVec4();
				sampl = new QVec4();

				q0.setQuat(keyframeAt(start));
				q1.setQuat(keyframeAt(end));
				sampl.setQuat(sample);

				sampl.slerpQuat(s, q0, q1);
				sample[0] = sampl.x;
				sample[1] = sampl.y;
				sample[2] = sampl.z;
				sample[3] = sampl.w;
				// may be not necessary
				temp = keyframeAt(start);
				temp[0] = q0.x;
				temp[1] = q0.y;
				temp[2] = q0.z;
				temp[3] = q0.w;
				temp = keyframeAt(end);
				temp[0] = q1.x;
				temp[1] = q1.y;
				temp[2] = q1.z;
				temp[3] = q1.w;
				break;
			case SPLINE:
				Start = keyframeAt(start);
				End = keyframeAt(end);
				tStart = tangentFrom(start);
				tEnd = tangentTo(end);

				s2 = s * s;
				s3 = s2 * s;

				for (int i = 0; i < componentCount; i++)
					sample[i] = (Start[i] * (((s3 * 2) - (3.f * s2)) + 1.f) + (End[i] * ((3.f * s2) - (s3 * 2)) + (tStart[i] * ((s3 - (s2 * 2)) + s) + (tEnd[i] * (s3 - s2)))));
				break;
			case SQUAD:
				if (componentCount != 4)
					throw new IllegalStateException();
				temp0 = new QVec4();
				temp1 = new QVec4();
				q0 = new QVec4();
				q1 = new QVec4();
				//A = new QVec4();
				//B = new QVec4();
				sampl = new QVec4();

				q0.setQuat(keyframeAt(start));
				q1.setQuat(keyframeAt(end));
				//A.setQuat(a[start]);
				//B.setQuat(b[end]);
				sampl.setQuat(sample);
				temp0.slerpQuat(s, q0, q1);
				temp1.slerpQuat(s, a[start], b[end]);
				sampl.slerpQuat(((s * (1.0f - s)) * 2), temp0, temp1);
				sample[0] = sampl.x;
				sample[1] = sampl.y;
				sample[2] = sampl.z;
				sample[3] = sampl.w;
				// may be not necessary
				temp = keyframeAt(start);
				temp[0] = q0.x;
				temp[1] = q0.y;
				temp[2] = q0.z;
				temp[3] = q0.w;
				temp = keyframeAt(end);
				temp[0] = q1.x;
				temp[1] = q1.y;
				temp[2] = q1.z;
				temp[3] = q1.w;
				/*temp = a[start];
				temp[0] = A.x;
				temp[1] = A.y;
				temp[2] = A.z;
				temp[3] = A.w;
				temp = b[end];
				temp[0] = B.x;
				temp[1] = B.y;
				temp[2] = B.z;
				temp[3] = B.w;*/
				break;
			default:
			       throw new IllegalStateException();
		}
		return 1;
	}


	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
		dirty = true;
	}

	public void setKeyframe(int index, int time, float[] value) {
		if (value == null) {
			throw new NullPointerException("Keyframe value vector must not be null");
		}

		if ((index < 0) || (index >= keyframeCount)) {
			throw new IndexOutOfBoundsException();
		}

		if ((value.length < componentCount) || (time < 0)) {
			throw new IllegalArgumentException();
		}

		System.arraycopy(value, 0, keyFrames[index], 0, componentCount);
		keyFrameTimes[index] = time;
		if (interpolationType == SLERP || interpolationType == SQUAD) {
			QVec4 q = new QVec4();
			float[] kf = keyframeAt(index);
			q.setQuat(kf);
			q.normalizeQuat();
			kf[0] = q.x;
			kf[1] = q.y;
			kf[2] = q.z;
			kf[3] = q.w;
		}
		dirty = true;
	}

	public int getKeyframe(int index, float[] value) {
		if ((index < 0) || (index >= keyframeCount)) {
			throw new IndexOutOfBoundsException();
		}
		
		if ((value != null) && (value.length < componentCount)) {
			throw new IllegalArgumentException();
		}

		if (value != null) {
			System.arraycopy(keyFrames[index], 0, value, 0, componentCount);
		}
		
		return keyFrameTimes[index];
	}

	public int getRepeatMode() {
		return repeatMode;
	}

	public void setRepeatMode(int repeatMode) {
		if (repeatMode != CONSTANT && repeatMode != LOOP)
			throw new IllegalArgumentException();

		this.repeatMode = repeatMode;
	}

	public int getValidRangeFirst() {
		return validRangeFirst;
	}

	public void setValidRange(int first, int last) {
		if ((first < 0) || (first >= keyframeCount) || (last < 0) || (last >= keyframeCount)) {
			throw new IndexOutOfBoundsException("Invalid range");
		}

		validRangeFirst = first;
		validRangeLast = last;
		dirty = true;
	}

	public int getComponentCount() {
		return componentCount;
	}

	public int getInterpolationType() {
		return interpolationType;
	}

	public int getKeyframeCount() {
		return keyframeCount;
	}

	public int getValidRangeLast() {
		return validRangeLast;
	}

}
