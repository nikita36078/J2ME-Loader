package javax.microedition.m3g;

import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.m3g.render.Renderer;

public final class Graphics3D {
	public static final int ANTIALIAS = 2;
	public static final int DITHER = 4;
	public static final int OVERWRITE = 16;
	public static final int TRUE_COLOR = 8;

	private static final String PROPERTY_SUPPORT_ANTIALIASING = "supportAntialiasing";
	private static final String PROPERTY_SUPPORT_TRUECOLOR = "supportTrueColor";
	private static final String PROPERTY_SUPPORT_DITHERING = "supportDithering";
	private static final String PROPERTY_SUPPORT_MIPMAPPING = "supportMipmapping";
	private static final String PROPERTY_SUPPORT_PERSPECTIVE_CORRECTION = "supportPerspectiveCorrection";
	private static final String PROPERTY_SUPPORT_LOCAL_CAMERA_LIGHTING = "supportLocalCameraLighting";
	private static final String PROPERTY_MAX_LIGHTS = "maxLights";
	private static final String PROPERTY_MAX_VIEWPORT_WIDTH = "maxViewportWidth";
	private static final String PROPERTY_MAX_VIEWPORT_HEIGHT = "maxViewportHeight";
	private static final String PROPERTY_MAX_VIEWPORT_DIMENSION = "maxViewportDimension";
	private static final String PROPERTY_MAX_TEXTURE_DIMENSION = "maxTextureDimension";
	private static final String PROPERTY_MAX_SPRITE_CROP_DIMENSION = "maxSpriteCropDimension";
	private static final String PROPERTY_MAX_TRANSFORM_PER_VERTEX = "maxTransformsPerVertex";
	private static final String PROPERTY_MAX_TEXTURE_UNITS = "numTextureUnits";

	private static Graphics3D instance = null;

	private int viewportX = 0;
	private int viewportY = 0;
	private int viewportWidth = 0;
	private int viewportHeight = 0;
	private Renderer renderer;

	private Camera camera;
	private Transform cameraTransform;

	private Vector lights = new Vector();
	private Vector lightTransforms = new Vector();
	private static boolean[] lightFlags;
	private int maxLights = 1;
	private boolean lightHasChanged = false;

	private boolean cameraHasChanged = false;

	private float depthRangeNear = 0;
	private float depthRangeFar = 1;
	private boolean depthRangeHasChanged = false;

	private boolean depthBufferEnabled;
	private boolean overwrite;
	private int hints;

	private static Hashtable implementationProperties = new Hashtable();

	private int width, height;

	private int clipX0, clipY0, clipX1, clipY1;
	private int scissorX, scissorY, scissorWidth, scissorHeight;

	private Graphics3D() {
		renderer = new Renderer();

	}

	public static Graphics3D getInstance() {
		if (instance == null) {
			instance = new Graphics3D();
		}
		return instance;
	}

	public void bindTarget(Object target) {
		bindTarget(target, true, 0);
	}

	public void bindTarget(Object target, boolean depthBuffer, int hints) {
		renderer.bindTarget(target, depthBuffer, hints);
	}

	public Object getTarget() {
		return renderer.getTarget();
	}

	public void releaseTarget() {
		renderer.releaseTarget();
	}

	public void clear(Background background) {
		if (renderer.getTarget() == null) {
			throw new IllegalStateException("Graphics3D does not have a rendering target");
		}

		renderer.clear(background);
	}

	public int addLight(Light light, Transform transform) {
		if (light == null)
			throw new NullPointerException("Light must not be null");

		lights.addElement(light);

		// Use identity transform if the given transform is null
		Transform t = new Transform();
		if (transform != null) {
			t.set(transform);
		}
		lightTransforms.addElement(t);

		int index = lights.size() - 1;

		// limit the number of lights
		if (index < maxLights) {
			lightFlags[index] = true;
		}

		lightHasChanged = true;
		return index;
	}

	public void setLight(int index, Light light, Transform transform) {
		lights.setElementAt(light, index);
		lightTransforms.setElementAt(transform, index);
		if (index < maxLights) {
			lightFlags[index] = true;
		}
		lightHasChanged = true;
	}

	public void resetLights() {
		lights.removeAllElements();
		lightTransforms.removeAllElements();
		for (int i = 0; i < maxLights; i++) {
			lightFlags[i] = false;
		}
		lightHasChanged = true;
	}

	public int getLightCount() {
		return lights.size();
	}

	public Light getLight(int index, Transform transform) {
		if (transform != null) {
			transform.set((Transform) lightTransforms.elementAt(index));
		}
		return (Light) lights.elementAt(index);
	}

	public int getHints() {
		return hints;
	}

	public boolean isDepthBufferEnabled() {
		return depthBufferEnabled;
	}

	private void setClipRect(int x, int y, int width, int height) {
		this.clipX0 = x;
		this.clipY0 = this.height - (y + height);
		this.clipX1 = clipX0 + width;
		this.clipY1 = clipY0 + height;
		updateScissor();
	}

	public void setViewport(int x, int y, int width, int height) {
		if ((width <= 0) || (height <= 0) || (width > renderer.getMaxViewportWidth()) ||
				(height > getViewportHeight())) {
			throw new IllegalArgumentException("Viewport coordinates are out of the allowed range");
		}

		this.viewportX = x;
		this.viewportY = this.height - (y + height);
		this.viewportWidth = width;
		this.viewportHeight = height;
		updateScissor();
	}

	private void updateScissor() {
		int sx0 = Math.max(viewportX, clipX0);
		int sy0 = Math.max(viewportY, clipY0);
		int sx1 = Math.min(viewportX + viewportWidth, clipX1);
		int sy1 = Math.min(viewportY + viewportHeight, clipY1);

		scissorX = sx0;
		scissorY = sy0;

		if (sx0 < sx1 && sy0 < sy1) {
			scissorWidth = sx1 - sx0;
			scissorHeight = sy1 - sy0;
		} else
			scissorWidth = scissorHeight = 0;
	}

	public int getViewportX() {
		return this.viewportX;
	}

	public int getViewportY() {
		return this.viewportY;
	}

	public int getViewportWidth() {
		return this.viewportWidth;
	}

	public int getViewportHeight() {
		return this.viewportHeight;
	}

	public void setDepthRange(float near, float far) {
		if ((near < 0) || (near > 1) || (far < 0) || (far > 1)) {
			throw new IllegalArgumentException("Bad depth range");
		}

		if ((depthRangeNear != near) || (depthRangeFar != far)) {
			depthRangeNear = near;
			depthRangeFar = far;
			depthRangeHasChanged = true;
		}
	}

	public float getDepthRangeNear() {
		return depthRangeNear;
	}

	public float getDepthRangeFar() {
		return depthRangeFar;
	}

	public static Hashtable getProperties() {
		// Force initialization of Graphics3D in order to populate implementationProperties
		if (instance == null) {
			getInstance();
		}
		return implementationProperties;
	}

	public void setCamera(Camera camera, Transform transform) {
		this.camera = camera;

		Transform t = new Transform();
		if (transform != null) {
			t.set(transform);
		}
		t.mtx.invertMatrix();
		this.cameraTransform = t;
		cameraHasChanged = true;
	}

	public Camera getCamera(Transform transform) {
		if (transform != null)
			transform.set(this.cameraTransform);
		return camera;
	}

	public void render(Node node, Transform transform) {
		if (renderer.getTarget() == null)
			throw new IllegalStateException("Graphics3D does not have a rendering target");
		if (camera == null)
			throw new IllegalStateException("Graphics3D does not have a current camera");

		// If the given transform is null, use the identity matrix
		if (transform == null) {
			transform = new Transform();
		}

		// Apply Graphics3D settings to the OpenGL pipeline
		initRender();

		if ((node instanceof Mesh) || (node instanceof Sprite3D) || (node instanceof Group)) {
			renderNode(node, transform);
		} else {
			throw new IllegalArgumentException("Node is not a Sprite3D, Mesh, or Group");
		}
	}

	private void initRender() {
		renderer.initRender(cameraHasChanged, lightHasChanged, depthRangeHasChanged);
		cameraHasChanged = false;
		lightHasChanged = false;
		depthRangeHasChanged = false;
	}

	public void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform) {
		if (vertices == null)
			throw new NullPointerException("vertices == null");
		if (triangles == null)
			throw new NullPointerException("triangles == null");
		if (appearance == null)
			throw new NullPointerException("appearance == null");
		if (renderer.getTarget() == null)
			throw new IllegalStateException("Graphics3D does not have a rendering target");
		if (camera == null)
			throw new IllegalStateException("Graphics3D does not have a current camera");
		// TODO Check if vertices or triangles violates the constraints defined in VertexBuffer or IndexBuffer

		// If the given transform is null, use the identity matrix
		if (transform == null) {
			transform = new Transform();
		}

		// Apply Graphics3D settings to the OpenGL pipeline
		initRender();

		renderer.render(vertices, triangles, appearance, transform);
	}

	public void render(VertexBuffer vertices, IndexBuffer triangles, Appearance appearance, Transform transform,
					   int scope) {
		// TODO: check scope
		render(vertices, triangles, appearance, transform);
	}

	public void render(World world) {
		if (renderer.getTarget() == null) {
			throw new IllegalStateException("Graphics3D does not have a rendering target");
		}

		clear(world.getBackground());

		Transform t = new Transform();

		// Setup camera
		Camera c = world.getActiveCamera();
		if (c == null)
			throw new IllegalStateException("World has no active camera.");
		if (!c.getTransformTo(world, t))
			throw new IllegalStateException("Camera is not in world.");

		// Camera
		setCamera(c, t);
		initRender();
		resetLights();
		populateLights(world, world);
		initRender();

		// Begin traversal of scene graph
		renderDescendants(world, world, new Transform());
	}

	private void populateLights(World world, Object3D obj) {
		int numReferences = obj.getReferences(null);
		if (numReferences > 0) {
			Object3D[] objArray = new Object3D[numReferences];
			obj.getReferences(objArray);
			for (int i = 0; i < numReferences; ++i) {
				if (objArray[i] instanceof Light) {
					Transform t = new Transform();
					Light light = (Light) objArray[i];
					if (light.isRenderingEnabled() && light.getTransformTo(world, t))
						addLight(light, t);
				}
				populateLights(world, objArray[i]);
			}
		}
	}

	private void renderDescendants(Group group, Object3D caller, Transform transform) {
		Node child = group.firstChild;
		if (child != null) {
			do {
				if (child != caller) {
					Transform t = new Transform();
					child.getCompositeTransform(t);
					t.mtx.preMultiplyMatrix(transform.mtx);
					renderNode(child, t);
				}
				child = child.right;
			} while (child != group.firstChild);
		}
	}

	private void renderNode(Node node, Transform transform) {
		if (!node.isRenderingEnabled()) {
			return;
		}
		if (node instanceof Mesh) {
			Mesh mesh = (Mesh) node;
			int subMeshes = mesh.getSubmeshCount();
			VertexBuffer vertices = mesh.getVertexBuffer();
			for (int i = 0; i < subMeshes; i++) {
				if (mesh.getAppearance(i) != null) {
					/*drawMesh*/
					render(vertices, mesh.getIndexBuffer(i), mesh.getAppearance(i), transform);
				}
			}
		} else if (node instanceof Sprite3D) {
			Sprite3D sprite = (Sprite3D) node;
			if (sprite.getAppearance() != null && sprite.getImage() != null && sprite.getCropWidth() != 0 && sprite.getCropHeight() != 0) {
				renderer.renderSprite(sprite, transform, this);
			}
		} else if (node instanceof Group) {
			renderDescendants((Group) node, node, transform);
		}

	}

	int getTextureUnitCount() {
		return renderer.getMaxTextureUnits();
	}

	int getMaxTextureSize() {
		return renderer.getMaxTextureSize();
	}

	void disableTextureUnits() {
		renderer.disableTextureUnits();
	}

}
