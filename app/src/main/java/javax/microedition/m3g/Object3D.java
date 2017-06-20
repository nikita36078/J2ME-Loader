package javax.microedition.m3g;

import java.util.Vector;

public abstract class Object3D {

	protected int userID = 0;
	protected Object userObject = null;

	Vector animationTracks = new Vector();

	void updateProperty(int property, float[] value) {
	}

	int applyAnimation(int time) {
		int validity = 0x7FFFFFFF;

		if (animationTracks == null)
			return validity;

		int numTracks = animationTracks.size();

		for (int trackIndex = 0; trackIndex < numTracks; ) {
			AnimationTrack track = (AnimationTrack) animationTracks.elementAt(trackIndex);
			KeyframeSequence sequence = track.sequence;

			int components = sequence.componentCount;
			int property = track.property;
			int nextProperty;

			int sumWeights = 0;
			float[] sumValues = new float[components];

			for (int i = 0; i < components; i++) sumValues[i] = 0;

			do {
				float[] weight = new float[1];
				int[] Validity = new int[1];

				track.getContribution(time, sumValues, weight, Validity);
				if (Validity[0] <= 0)
					return 0;

				sumWeights += weight[0];
				validity = (validity <= Validity[0]) ? validity : Validity[0];

				if (++trackIndex == numTracks)
					break;
				track = (AnimationTrack) animationTracks.elementAt(trackIndex);
				nextProperty = track.property;
			} while (nextProperty == property);

			if (sumWeights > 0)
				updateProperty(property, sumValues);
		}
		return validity;
	}

	int doGetReferences(Object3D[] references) {
		if (!animationTracks.isEmpty()) {
			if (references != null) {
				for (int i = 0; i < animationTracks.size(); ++i) {
					references[i] = (Object3D) animationTracks.elementAt(i);
				}
			}
			return animationTracks.size();
		}
		return 0;
	}

	public final Object3D duplicate() {
		return duplicateImpl();
	}

	abstract Object3D duplicateImpl();

	Object3D findID(int userID) {
		if (this.userID == userID)
			return this;

		if (animationTracks != null)
			for (int i = 0; i < animationTracks.size(); i++) {
				AnimationTrack track = (AnimationTrack) animationTracks.elementAt(i);
				Object3D found = track.findID(userID);
				if (found != null)
					return found;
			}

		return null;
	}
	public Object3D find(int userID) {
		if (this.userID == userID)
			return this;

		return findID(userID);
	}

	public int getReferences(Object3D[] references) throws IllegalArgumentException {
		return doGetReferences(references);
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}

	public Object getUserObject() {
		return this.userObject;
	}

	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}

	public void addAnimationTrack(AnimationTrack animationTrack) {
		
		if (animationTrack == null) {
			throw new NullPointerException();
		}
		if (/*(!isCompatible(animationTrack)) ||*/ animationTracks.contains(animationTrack)) {
			throw new IllegalArgumentException("AnimationTrack is already existing or incompatible");
		}
			
		int newTrackTarget = animationTrack.getTargetProperty();
		int components = animationTrack.getKeyframeSequence().getComponentCount();
		int i;
		for (i = 0; i < animationTracks.size(); i++) {
			AnimationTrack track = (AnimationTrack) animationTracks.elementAt(i);

			if (track.getTargetProperty() > newTrackTarget)
				break;

			if (track.getTargetProperty() == newTrackTarget && (track.getKeyframeSequence().getComponentCount() != components)) {
				throw new IllegalArgumentException();
			}
		}
		
		animationTracks.add(i, animationTrack);
	}

	public AnimationTrack getAnimationTrack(int index) {
		return (AnimationTrack) animationTracks.elementAt(index);
	}

	public void removeAnimationTrack(AnimationTrack animationTrack) {
		animationTracks.removeElement(animationTrack);
	}

	public int getAnimationTrackCount() {
		return animationTracks.size();
	}

	public final int animate(int time) {
		return applyAnimation(time);
	}
	
	boolean isCompatible(AnimationTrack animationtrack) {
		return false;
	}

}
