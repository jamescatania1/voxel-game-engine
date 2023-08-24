package james;

import java.util.ArrayList;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

public class CullingQuadTree implements GameObject, WindowResizeListener {
    private static final int WORLD_SIZE = 512;
    private static final float WORLD_Y_MIN = 0.0f;
    private static final float WORLD_Y_MAX = 8.0f;
    private static final float PADDING_X_POS = 20.0f;
    private static final float PADDING_Z_POS = 0.0f;
    private static final float PADDING_X_NEG = 20.0f;
    private static final float PADDING_Z_NEG = 2.0f;
    private static final float CAMERA_MOVE_UPDATE_THRESHOLD = 1.5f;
    private static final float CAMERA_DISTANCE_UPDATE_THRESHOLD = 1.5f;

    public static CullingQuadTree instance;
    public boolean treeUpdated;
    
    private QuadTreeNode root;
    private ArrayList<CullVolume> volumes;
    private boolean cullVolumesChanged;

    private Vector3f lastUpdateCameraPos;
    private float lastUpdateCameraDistance;

    public CullingQuadTree(){
        Game.AddObject(this);
        Game.AddWindowResizeListener(this);
        instance = this;
        volumes = new ArrayList<>();
        root = new QuadTreeNode(0, 0, WORLD_SIZE);
        cullVolumesChanged = true;
    }

    public void UpdateCullTree(){
        //set all volumes to be culled
        for(CullVolume volume : volumes){
            volume.isCulled = true;
        }
        UpdateCullTreeRecur(root);
        
        //set all volumes' previous cull values and check if tree was updated (cull-culling!)
        treeUpdated = false;
        for(CullVolume volume : volumes){
            if(volume.prevIsCulled != volume.isCulled) treeUpdated = true;
            volume.prevIsCulled = volume.isCulled;
        }
    }
    public void UpdateCullTreeRecur(QuadTreeNode node){
        if(node.isCulled()) return;

        for(CullVolume volume : node.volumes){
            if(!volume.prevIsCulled) treeUpdated = true;
            volume.isCulled = false;
        }
        
        if(node.children == null) return;
        else for(int i = 0; i < 4; i++) UpdateCullTreeRecur(node.children[i]);
    }

    public CullVolume AddVolume(int x, int z, int w){
        cullVolumesChanged = true;

        if(x < 0 || x + w > WORLD_SIZE || z < 0 || z + w > WORLD_SIZE) 
            throw new Error("culling volume (" + String.valueOf(x) + ", " + String.valueOf(z) + ") x " + String.valueOf(w) + " is outside world bounds");

        //calculate bounding quad tree box
        Vector3i bdBox = calculateVolumeBox(x, z, w);
        CullVolume volume = new CullVolume(bdBox.x, bdBox.y, bdBox.z);
        
        //add the volume to the quad tree
        int bdX = bdBox.x; int bdZ = bdBox.y; int bdW = bdBox.z;
        QuadTreeNode current = root;
        while(current.x != bdX || current.z != bdZ || current.w != bdW){
            if(bdX < current.x + current.w / 2 && bdZ < current.z + current.w / 2){
                current = current.children[0];
            }
            else if(bdX < current.x + current.w / 2){
                current = current.children[2];
            }
            else if(bdZ  < current.z + current.w / 2){
                current = current.children[1];
            }
            else current = current.children[3];
        }
        current.volumes.add(volume);
        volumes.add(volume);

        return volume;
    }

    public void RemoveVolumeExact(CullVolume volume){
        cullVolumesChanged = true;

        //remove the volume from the quad tree
        int bdX = volume.x; int bdZ = volume.z; int bdW = volume.w;
        QuadTreeNode current = root;
        while(current.x != bdX || current.z != bdZ || current.w != bdW){
            if(bdX + bdW < current.x + current.w / 2 && bdZ + bdW < current.z + current.w / 2){
                current = current.children[0];
            }
            else if(bdX + bdW < current.x + current.w / 2){
                current = current.children[2];
            }
            else if(bdZ + bdW < current.z + current.w / 2){
                current = current.children[1];
            }
            else current = current.children[3];
        }
        current.volumes.remove(volume);
        volumes.remove(volume);
    }

    private Vector3i calculateVolumeBox(int x, int z, int w){
        int bdW = Integer.highestOneBit(Math.max(1, w - 1)) * 2;
        return new Vector3i((x / bdW) * bdW, (z / bdW) * bdW, bdW);
    }

    public class CullVolume {
        public boolean isCulled;
        private boolean prevIsCulled;
        public int x, z, w;
        public CullVolume(int x, int z, int w){
            this.x = x; this.z = z; this.w = w;
        }
    }

    private class QuadTreeNode {
        public ArrayList<CullVolume> volumes;
        public QuadTreeNode[] children;
        public int x, z, w;
        private Vector4f[] boxVerts;

        public QuadTreeNode(int x, int z, int w){
            this.x = x; this.z = z; this.w = w;
            volumes = new ArrayList<>();
            if(w > 2) {
                children = new QuadTreeNode[4];
                children[0] = new QuadTreeNode(x, z, w / 2);
                children[1] = new QuadTreeNode(x + w / 2, z, w / 2);
                children[2] = new QuadTreeNode(x, z + w / 2, w / 2);
                children[3] = new QuadTreeNode(x + w / 2, z + w / 2, w / 2);                
            }
            boxVerts = new Vector4f[]{
                new Vector4f(-PADDING_X_NEG + (float)x, WORLD_Y_MIN, -PADDING_Z_NEG + (float)z, 1.0f),
                new Vector4f(PADDING_X_POS + (float)(x + w), WORLD_Y_MIN, PADDING_Z_POS + (float)(z + w), 1.0f),
                new Vector4f(-PADDING_X_NEG + (float)x, WORLD_Y_MIN, PADDING_Z_POS + (float)(z + w), 1.0f),
                new Vector4f(PADDING_X_POS + (float)(x + w), WORLD_Y_MIN, -PADDING_Z_NEG + (float)z, 1.0f),
                new Vector4f(-PADDING_X_NEG + (float)x, WORLD_Y_MAX, -PADDING_Z_NEG + (float)z, 1.0f),
                new Vector4f(PADDING_X_POS + (float)(x + w), WORLD_Y_MAX, PADDING_Z_POS + (float)(z + w), 1.0f),
                new Vector4f(-PADDING_X_NEG + (float)x, WORLD_Y_MAX, PADDING_Z_POS + (float)(z + w), 1.0f),
                new Vector4f(PADDING_X_POS + (float)(x + w), WORLD_Y_MAX, -PADDING_Z_NEG + (float)z, 1.0f),
            };
        }

        public boolean isCulled(){
            //transform each corner of the bounding box's vertices, and check screen coordinates
            boolean[] sidesCovered = new boolean[6];
            Vector4f scVert = new Vector4f();
            for(int i = 0; i < 8; i++){
                boxVerts[i].mul(Camera.instance.viewProjMatrix, scVert);
                if(scVert.x > -1.0f) sidesCovered[0] = true;
                if(scVert.x < 1.0f) sidesCovered[1] = true;
                if(scVert.y > -1.0f) sidesCovered[2] = true;
                if(scVert.y < 1.0f) sidesCovered[3] = true;
            }
            for(int i = 0; i < 4; i++) if(!sidesCovered[i]) return true;
            return false;
        }
    }

    public void Update() {
    }
    
    public void FixedUpdate() {
        if((cullVolumesChanged || Camera.instance.position.distance(lastUpdateCameraPos) > CAMERA_MOVE_UPDATE_THRESHOLD ||
        Math.abs(Camera.instance.distance - lastUpdateCameraDistance) > CAMERA_DISTANCE_UPDATE_THRESHOLD) && !Input.GetKeyDown(Input.KEY_SPACE)){
            UpdateCullTree();
            cullVolumesChanged = false;
            lastUpdateCameraPos = new Vector3f(Camera.instance.position);
            lastUpdateCameraDistance = Camera.instance.distance;
        }
    }

    public void Draw() {
    }


    // Static wrappers

    /**
     * Creates and adds a new CullVolume that contains the dimensions to the active cull tree.
     * @param x
     * @param z
     * @param width
     * @param depth
     * @return
     *      - a CullVolume buffer for the volume's cull state.
     */
    public static CullVolume AddVolume(int x, int z, int width, int depth){
        return instance.AddVolume(x, z, Math.max(width, depth));
    }

    /**
     * Removes the CullVolume object. Call before destroying the associated object.
     * @param x
     * @param z
     * @param width
     * @param depth
     */
    public static void RemoveVolume(CullVolume volume){
        instance.RemoveVolumeExact(volume);
    }

    public void OnWindowResize() {
        cullVolumesChanged = true;
    }
}