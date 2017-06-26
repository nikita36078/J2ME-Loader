package javax.microedition.m3g;


public class World extends Group {
	private Camera activeCamera;
	private Background background;

	public World() {
	}

	Object3D duplicateImpl() {
		super.duplicateImpl();
		World copy = new World();
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

	public int getReferences(Object3D[] references) throws IllegalArgumentException {
		int parentCount = super.getReferences(references);

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
}
