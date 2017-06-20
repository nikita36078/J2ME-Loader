package javax.microedition.m3g;

public class AnimationController extends Object3D {
	
	private int activeIntervalStart = 0;
	private int activeIntervalEnd = 0;
	private float speed = 1.0f;
	private int referenceWorldTime = 0;
	private float referenceSequenceTime = 0;
	private float weight = 1.0f;
	
	Object3D duplicateImpl() {
		AnimationController copy = new AnimationController();
		copy.activeIntervalStart = activeIntervalStart;
		copy.activeIntervalEnd = activeIntervalEnd;
		copy.speed = speed;
		copy.referenceWorldTime = referenceWorldTime;
		copy.referenceSequenceTime = referenceSequenceTime;
		copy.weight = weight;
		return copy;
	}
	
	int timeToActivation(int worldTime) {
		if (worldTime < activeIntervalStart)
			return activeIntervalStart - worldTime;
		else if (worldTime < activeIntervalEnd)
			return 0;

		return 0x7FFFFFFF;
	}

	int timeToDeactivation(int worldTime) {
		if (worldTime < activeIntervalEnd)
			return activeIntervalEnd - worldTime;
		return 0x7FFFFFFF;
	}

	boolean isActive(int worldTime) {
		if (activeIntervalStart == activeIntervalEnd)
			return true;
		return (worldTime >= activeIntervalStart && worldTime < activeIntervalEnd);
	}

	public void setActiveInterval(int start, int end) {
		if (start > end)
			throw new IllegalArgumentException("Start time must be inferior to end time");
		
		activeIntervalStart = start;
		activeIntervalEnd = end;
	}
	
	public int getActiveIntervalStart() {
		return activeIntervalStart;
	}
	
	public int getActiveIntervalEnd() {
		return activeIntervalEnd;
	}
	
	public void setSpeed(float speed, int worldTime) {
		this.referenceSequenceTime = getPosition(worldTime);
		this.referenceWorldTime = worldTime;
		this.speed = speed;
	}
	
	public float getSpeed() {
		return speed;
	}
	
	public void setPosition(float sequenceTime, int worldTime) {
		this.referenceSequenceTime = sequenceTime;
		this.referenceWorldTime = worldTime;
	}
	
	public float getPosition(int worldTime) {
		return (referenceSequenceTime + (speed * (float)(worldTime - referenceWorldTime)));
	}
	
	public int getRefWorldTime() {
		return referenceWorldTime;
	}
	
	public void setWeight(float weight) {
		
		if (weight < 0) {
	    	 throw new IllegalArgumentException("Weight must be positive or zero");
	    }
		
		this.weight = weight;
	}
	
	public float getWeight() {
		return weight;
	}

}
