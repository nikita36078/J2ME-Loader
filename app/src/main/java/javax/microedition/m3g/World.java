package javax.microedition.m3g;


public class World extends Group {
	private Camera activeCamera;
	private Background background;

	public World() {
	}

	Object3D duplicateImpl() {
		World copy = new World();
		super.duplicate((Group) copy);
		copy.activeCamera = activeCamera;
		copy.background = background;
		return copy;
	}

	public Camera getActiveCamera() {
		return activeCamera;
	}

	public void setActiveCamera(Camera camera) {
		activeCamera = camera;
	}

	public Background getBackground() {
		return background;
	}

	public void setBackground(Background background) {
		this.background = background;
	}

	@Override
	public int doGetReferences(Object3D[] references) {
		int parentCount = super.doGetReferences(references);

		if (activeCamera != null) {
			if (references != null)
				references[parentCount] = activeCamera;
			++parentCount;
		}

		if (background != null) {
			if (references != null)
				references[parentCount] = background;
			++parentCount;
		}

		return parentCount;
	}

	@Override
	Object3D findID(int userID) {
		Object3D found = super.findID(userID);

		if ((found == null) && (activeCamera != null))
			found = activeCamera.findID(userID);
		if ((found == null) && (background != null))
			found = background.findID(userID);
		return found;
	}

	@Override
	int applyAnimation(int time) {
		int minValidity = super.applyAnimation(time);
		if ((background != null) && (minValidity > 0)) {
			int validity = background.applyAnimation(time);
			minValidity = Math.min(validity, minValidity);
		}
		return minValidity;
	}
}
