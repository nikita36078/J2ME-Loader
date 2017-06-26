package javax.microedition.m3g;

public class AnimationController extends Object3D {

	private int activationTime = 0;
	private int deactivationTime = 0;
	private float speed = 1.0f;
	private int refWorldTime = 0;
	private float refSequenceTime = 0;
	private float weight = 1.0f;

	Object3D duplicateImpl() {
		AnimationController copy = new AnimationController();
		copy.activationTime = activationTime;
		copy.deactivationTime = deactivationTime;
		copy.speed = speed;
		copy.refWorldTime = refWorldTime;
		copy.refSequenceTime = refSequenceTime;
		copy.weight = weight;
		return copy;
	}

	int timeToActivation(int worldTime) {
		if (worldTime < activationTime)
			return activationTime - worldTime;
		else if (worldTime < deactivationTime)
			return 0;

		return 0x7FFFFFFF;
	}

	int timeToDeactivation(int worldTime) {
		if (worldTime < deactivationTime)
			return deactivationTime - worldTime;
		return 0x7FFFFFFF;
	}

	boolean isActive(int worldTime) {
		if (activationTime == deactivationTime)
			return true;
		return (worldTime >= activationTime && worldTime < deactivationTime);
	}

	public void setActiveInterval(int start, int end) {
		if (start > end)
			throw new IllegalArgumentException("Start time must be inferior to end time");

		activationTime = start;
		deactivationTime = end;
	}

	public int getActiveIntervalStart() {
		return activationTime;
	}

	public int getActiveIntervalEnd() {
		return deactivationTime;
	}

	public void setSpeed(float speed, int worldTime) {
		this.refSequenceTime = getPosition(worldTime);
		this.refWorldTime = worldTime;
		this.speed = speed;
	}

	public float getSpeed() {
		return speed;
	}

	public void setPosition(float sequenceTime, int worldTime) {
		this.refSequenceTime = sequenceTime;
		this.refWorldTime = worldTime;
	}

	public float getPosition(int worldTime) {
		return (refSequenceTime + (speed * (float) (worldTime - refWorldTime)));
	}

	public int getRefWorldTime() {
		return refWorldTime;
	}

	public void setWeight(float weight) {
		if (weight < 0)
			throw new IllegalArgumentException("Weight must be positive or zero");
		this.weight = weight;
	}

	public float getWeight() {
		return weight;
	}

}
