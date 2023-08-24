package james;

import static org.lwjgl.opengl.GL44C.*;

import java.util.ArrayList;

public class VoxelInstanceRenderer {
    
    private ArrayList<Instance> instances;
    
    private int VAO;
    private int VBO;
    private int EBO;
    private int indexCount;
    private int shadowIndexCount;
    private int activeInstanceCount;
    private int modelWidth, modelDepth;
    private int instanceVBO;

    private boolean requiresUpdate;

    public VoxelInstanceRenderer(String meshName){
        this(VoxelLoader.meshObjects.get(meshName));
    }

    public VoxelInstanceRenderer(VoxelData mesh){
        instances = new ArrayList<>();

        VAO = glGenVertexArrays();
        glBindVertexArray(VAO);
        EBO = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        VBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 16, (long)0);
        glVertexAttribPointer(1, 1, GL_FLOAT, false, 16, (long)(12));
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        
        instanceVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
        glVertexAttribIPointer(2, 1, GL_INT, 0, 0);
        glVertexAttribDivisor(2, 1);
        glEnableVertexAttribArray(2);

        RenderMeshData renderMesh = new RenderMeshData(mesh);

        //vertexCount = renderMesh.vertexCount;
        indexCount = renderMesh.culledIndexCount;
        shadowIndexCount = renderMesh.completeIndexCount;
        modelWidth = renderMesh.width;
        modelDepth = renderMesh.depth;

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, renderMesh.indices, GL_STATIC_DRAW);
        
        glBindBuffer(GL_ARRAY_BUFFER, VBO);
        glBufferData(GL_ARRAY_BUFFER, renderMesh.vertices, GL_STATIC_DRAW);

        UpdateInstanceData();
    }

    public void AddInstance(int x, int z){
        requiresUpdate = true;
        instances.add(new Instance(x, z, modelWidth, modelDepth));
    }

    public void RemoveInstance(int x, int z){
        requiresUpdate = true;
        instances.remove(new Instance(x, z, modelWidth, modelDepth));
    }

    public void UpdateInstanceData(){
        int[] instanceData = new int[instances.size()];

        int renderIndex = 0;
        for(Instance instance : instances){
            if(instance.cullVolume.isCulled) continue;
            
            instance.PackInstanceData();
            instanceData[renderIndex] = instance.instanceData;
            renderIndex++;
        }
        activeInstanceCount = renderIndex;
        
        glBindVertexArray(VAO);
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
        glBufferData(GL_ARRAY_BUFFER, instanceData, GL_STATIC_DRAW);
    }

    public void Draw() {
        if(instances.isEmpty()) return;
        
        if(requiresUpdate || CullingQuadTree.instance.treeUpdated) UpdateInstanceData();
        requiresUpdate = false;

        glBindVertexArray(VAO);
        glDrawElementsInstanced(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0, activeInstanceCount);
    }

    public void DrawShadowPass() {
        if(instances.isEmpty()) return;
        
        if(requiresUpdate || CullingQuadTree.instance.treeUpdated) UpdateInstanceData();
        requiresUpdate = false;
        
        glBindVertexArray(VAO);
        glDrawElementsInstanced(GL_TRIANGLES, shadowIndexCount, GL_UNSIGNED_INT, 0, activeInstanceCount);
    }

    private class Instance {
        public int x, z;
        public CullingQuadTree.CullVolume cullVolume;

        public int instanceData;

        public Instance(int x, int z, int width, int depth){
            this.x = x; this.z = z;
            this.cullVolume = CullingQuadTree.AddVolume(x, z, width, depth);
        }
        
        public void PackInstanceData(){
            instanceData = (x & 0x1FF) << 23 | (z & 0x1FF) << 14;
        }

        public int hashCode(){
            return instanceData;
        }
    }
}
