package james;

import static james.Input.*;

import static org.lwjgl.opengl.GL44C.*;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import james.GameScene.GameState;


public class Camera implements GameObject{
    public static final float ZNEAR = -1.0f;
    public static final float ZFAR = 200.0f;
    
    public static final float WORLD_SIZE = 512.0f;
    private final float ZOOM_DEFAULT = 20.0f;
    private final float ZOOM_MIN = 0.1f;
    private final float ZOOM_MAX = 30.0f;
    private final float ZOOM_SPEED = 16.0f;
    private final float ZOOM_SMOOTHING = 140.0f;
    private final float ZOOM_SMOOTHING_START = 1.5f;

    private final float MOVE_SMOOTHING = 140.0f;
    private final float MOVE_SPEED = 40.0f;

    private final float SHADOWMAP_PADDING_WIDTH = 1.4f;
    private final float SHADOWMAP_PADDING_HEIGHT = 2.6f;
    private final float SHADOWMAP_MINIMUM_ZOOM = 5f;

    public static Camera instance;
    
    private int camDataUBO;
    public Matrix4f projMatrix;
    public Matrix4f viewMatrix;
    public Matrix4f viewProjMatrix;
    public Matrix4f sunViewProjMatrix;
    
    public Vector3f position;
    private Vector3f targPosition;
    private float targDistance = ZOOM_DEFAULT;
    public float distance = ZOOM_MAX;
    private boolean startZoomActive = true;
    private Vector2f scSize;

    public boolean movedLastUpdate = true;
    private Vector3f prevUpdatePosition;
    private float prevUpdateDistance;

    public Camera(){
        instance = this;
        Game.AddObject(this);

        projMatrix = new Matrix4f();
        viewMatrix = new Matrix4f();
        viewProjMatrix = new Matrix4f();
        sunViewProjMatrix = new Matrix4f();
        position = new Vector3f(0.5f * WORLD_SIZE, 0.0f, 0.5f * WORLD_SIZE);
        targPosition = new Vector3f(position);

        //create global buffer for camera matrices
        camDataUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, camDataUBO);
        glBufferData(GL_UNIFORM_BUFFER, (long)272, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_UNIFORM_BUFFER, 2, camDataUBO);

        Update();
    }
    
    private float shadowmapSize = 1.0f;
    public void Update() {
        //camera distance smoothing
        if(startZoomActive) distance += (targDistance - distance) * Math.min(ZOOM_SMOOTHING_START * Game.deltaTime, 0.5f);
        else distance += (targDistance - distance) * Math.min(Game.deltaTime * ZOOM_SMOOTHING, 0.5f);

        if(GameScene.state != GameState.Paused){
            float xVel = 0.0f; float zVel = 0.0f;
            if(Input.GetKeyDown(KEY_D)) xVel += 1.0f;
            if(Input.GetKeyDown(KEY_A)) xVel -= 1.0f;
            if(Input.GetKeyDown(KEY_W)) zVel += 1.0f;
            if(Input.GetKeyDown(KEY_S)) zVel -= 1.0f;
            xVel /= Math.sqrt(Math.max(1.0f, xVel * xVel + zVel * zVel));
            zVel /= Math.sqrt(Math.max(1.0f, xVel * xVel + zVel * zVel));
            zVel *= 1.75;
            
            float zoomPanMultiplier = (Math.min(Math.max(targDistance, ZOOM_MIN * 9f), ZOOM_MAX / 1.9f) - ZOOM_MIN) / (ZOOM_MAX - ZOOM_MIN);
            targPosition.x += xVel * zoomPanMultiplier * MOVE_SPEED * Game.deltaTime;
            targPosition.z += zVel * zoomPanMultiplier * MOVE_SPEED * Game.deltaTime;
        }

        position.x += (targPosition.x - position.x) * Math.min(Game.deltaTime * MOVE_SMOOTHING, 0.5f);
        position.z += (targPosition.z - position.z) * Math.min(Game.deltaTime * MOVE_SMOOTHING, 0.5f);

        float aspect = (float)Main.windowX / (float)Main.windowY;
        float desAspect = (float)Main.DEFAULT_WINDOW_WIDTH / (float)Main.DEFAULT_WINDOW_HEIGHT;
        float scWidth = Math.min(1.0f, desAspect);
        float scHeight = Math.min(1.0f, 1.0f / desAspect);
        scWidth = Math.min(scWidth, scWidth * aspect / desAspect);
        scHeight = Math.min(scHeight, scHeight * desAspect / aspect);
        
        projMatrix = new Matrix4f();
        scSize = new Vector2f(2.0f * scWidth, 2.0f * scHeight);
        projMatrix.ortho(scWidth * distance, -scWidth * distance, -scHeight * distance, scHeight * distance, ZNEAR, ZFAR, projMatrix);
        projMatrix.lookAt(-1.0f, 1.0f, -1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, projMatrix);

        viewMatrix = new Matrix4f();
        viewMatrix.translate(-position.x - position.z + WORLD_SIZE * 0.5f, 0.0f, -position.z + position.x - WORLD_SIZE * 0.5f, viewMatrix);

        viewProjMatrix = projMatrix.mul(viewMatrix);

        //sun matrix
        if(!Input.GetKeyDown(Input.KEY_LEFT_SHIFT)) {
            shadowmapSize = Math.max(distance, SHADOWMAP_MINIMUM_ZOOM);
        }

        float shadowmapWidth = SHADOWMAP_PADDING_WIDTH * desAspect * Math.min(1.0f, scHeight);
        float shadowmapHeight = SHADOWMAP_PADDING_HEIGHT * Math.min(1.0f, scWidth) / desAspect;
        float angleHeightFactor = (float)(Math.sin(Lighting.instance.activeShadowRotation));
        sunViewProjMatrix = new Matrix4f();
        sunViewProjMatrix.ortho(
            shadowmapSize * shadowmapWidth, -shadowmapSize * shadowmapWidth, 
            -shadowmapSize * shadowmapHeight * angleHeightFactor, shadowmapSize * shadowmapHeight * angleHeightFactor, 
            50.0f, ZFAR, sunViewProjMatrix);
        sunViewProjMatrix.lookAt(
            Lighting.instance.activeShadowDirection.x * 100.0f, Lighting.instance.activeShadowDirection.y * 100.0f, Lighting.instance.activeShadowDirection.z * 100.0f, 
            0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, sunViewProjMatrix);

        float shadowX = position.x - 0.2f * shadowmapSize * shadowmapHeight * (float)Math.cos(Lighting.instance.activeShadowRotation);
        float shadowZ = position.z - 0.05f * shadowmapSize * shadowmapWidth;
        Matrix4f sunViewMatrix = new Matrix4f();
        sunViewMatrix.translate(-shadowX - shadowZ + WORLD_SIZE * 0.5f, 0.0f, -shadowZ + shadowX - WORLD_SIZE * 0.5f, sunViewMatrix);

        sunViewProjMatrix = sunViewProjMatrix.mul(sunViewMatrix);

        //set the global buffer data for camera data.
        float[] viewMatrixData = new float[16];
        viewMatrix.get(viewMatrixData);
        float[] projMatrixData = new float[16];
        projMatrix.get(projMatrixData);
        float[] viewProjMatrixData = new float[16];
        viewProjMatrix.get(viewProjMatrixData);
        float[] sunViewProjMatrixData = new float[16];
        sunViewProjMatrix.get(sunViewProjMatrixData);
        float[] camData = new float[68];
        for(int i = 0; i < 16; i++){
            camData[i + 0] = viewMatrixData[i];
            camData[i + 16] = projMatrixData[i];
            camData[i + 32] = viewProjMatrixData[i];
            camData[i + 48] = sunViewProjMatrixData[i];
        }
        camData[64] = ZNEAR; camData[65] = ZFAR; camData[66] = distance; camData[67] = (float)Main.windowX / (float)Main.windowY;
        glBindBuffer(GL_UNIFORM_BUFFER, camDataUBO);
        glBufferSubData(GL_UNIFORM_BUFFER, (long)0, camData);

        if(movedLastUpdate){
            movedLastUpdate = false;
            prevUpdatePosition = new Vector3f(position);
            prevUpdateDistance = distance;
        }
        else if(Math.abs(distance - prevUpdateDistance) > 0.01f || position.distance(prevUpdatePosition) > 0.01f) movedLastUpdate = true;


        //float scWorldMaxWidth = scSize.x * ZOOM_MAX / (float)Math.sqrt(2.0);
        //float scWorldMaxHeight = scSize.y * ZOOM_MAX * (float)Math.sqrt(3.0) / (float)Math.sqrt(2.0);

        float scWorldWidth = scSize.x * targDistance / (float)Math.sqrt(2.0);
        float scWorldHeight = scSize.y * targDistance * (float)Math.sqrt(3.0) / (float)Math.sqrt(2.0);
        //System.out.println(0.5f * scWorldHeight) + ", " + String.valueOf(targPosition.z + 0.5f * scWorldHeight));

        float maxRadius = WORLD_SIZE * 0.25f + 3.0f;
        if(!Input.GetKeyDown(Input.KEY_T)) targPosition.x = (float)Math.min(WORLD_SIZE * 0.5f + maxRadius - 0.5f * scWorldWidth, Math.max(WORLD_SIZE * 0.5 - maxRadius + 0.5f * scWorldWidth, targPosition.x));
        if(!Input.GetKeyDown(Input.KEY_T)) targPosition.z = (float)Math.min(WORLD_SIZE * 0.5f + maxRadius - 0.5f * scWorldHeight, Math.max(WORLD_SIZE * 0.5 - maxRadius + 0.5f * scWorldHeight, targPosition.z));
    }

    public void FixedUpdate() {
        if(GameScene.state == GameState.Paused) return;

        if(startZoomActive && Math.abs(Input.scrollVal) > 0.001){
            startZoomActive = false;
            targDistance = distance;
        }

        float zoomSpeedMultiplier = (Math.min(Math.max(targDistance, ZOOM_MIN * 6f), ZOOM_MAX / 1.6f) - ZOOM_MIN) / (ZOOM_MAX - ZOOM_MIN);

        float prevTargDistance = targDistance;
        targDistance -= ZOOM_SPEED * zoomSpeedMultiplier * Input.scrollVal;
        targDistance = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, targDistance));
        float distanceChange = targDistance - prevTargDistance;

        targPosition.x -= scSize.x * (float)Input.mouseX * 0.5f * distanceChange / (float)Math.sqrt(2.0);
        targPosition.z -= scSize.y * (float)Input.mouseY * 0.5f * distanceChange * (float)Math.sqrt(3.0) / (float)Math.sqrt(2.0);
    }

    public void Draw() {
    }
}
