package james;

import static org.lwjgl.opengl.GL44C.*;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Vector2i;

public class SceneRenderer implements GameObject, WindowResizeListener{
    public static SceneRenderer instance;
    public static enum RendererVariant { Standard, Foliage };

    private static final String[] FOLIAGE_NAMES = { "tree_cedar" };

    public HashMap<String, VoxelInstanceRenderer> instanceRenderers;
    public HashMap<String, RendererVariant> variantFlags;

    private int frameBuffer;
    private int renderBuffer;
    private int colorBufferTexture;
    private int colorBufferTextureUnit;
    private int renderedSceneFramebuffer;
    private int renderedSceneTexture;
    private int renderedSceneTextureUnit;
    private int shadowmapFrameBuffer;
    private int shadowmapTexture;
    private int shadowmapTextureUnit;
    private int foliageWindFieldTexture;
    private int foliageWindFieldTextureUnit;
    private int foliageWindFieldOffsetTexture;
    private int foliageWindFieldOffsetTextureUnit;
    private int blurDownsampleInputTexture;
    private int blurDownsampleInputTextureUnit;
    private int blurHorizontalInputTexture;
    private int blurHorizontalInputTextureUnit;
    private int blurVerticalInputTexture;
    private int blurVerticalInputTextureUnit;
    private ScreenQuad screenQuad;

    //private int bloomMipFrameBuffer;
    private int[] bloomMipTextures;
    private int[] bloomMipTextureUnits;
    private Vector2i[] bloomMipSizes;
    private Shader bloomDownsampleShader;
    private Shader bloomUpsampleShader;
    private Shader postProcessingShader;
    
    private Shader worldShader;
    private Shader worldShadowmapShader;
    private Shader foliageWindFieldShader;
    private Shader foliageShader;
    private Shader foliageShadowmapShader;
    private Shader gaussianBlurShader;

    private float blurAmount;
    private boolean blurEnabled;
    private float blurTarget;
    private float blurSpeed;

    private static final boolean BLOOM_ENABLED = true;
    private static final int BLOOM_MIP_LEVELS = 4;
    private static final float BLOOM_RADIUS = 0.85f;
    private static final float BLOOM_INTENSITY = 0.41f;
    private static final int BLUR_PASSES = 2;

    public SceneRenderer(){
        instance = this;
        Game.AddObject(this);
        Game.AddWindowResizeListener(this);

        screenQuad = new ScreenQuad(new Shader("fx.vert", "fx.frag"));
        worldShader = new Shader("world.vert", "world.frag");
        worldShadowmapShader = new Shader("shadowmap.vert", "shadowmap.frag");
        bloomDownsampleShader = new Shader("bloomdownsample.comp");
        bloomUpsampleShader = new Shader("bloomupsample.comp");
        foliageWindFieldShader = new Shader("foliageWindField.comp");
        foliageShader = new Shader("foliage.vert", "world.frag");
        foliageShadowmapShader = new Shader("foliageShadowmap.vert", "shadowmap.frag");
        gaussianBlurShader = new Shader("fastgaussian.comp");
        postProcessingShader = new Shader("postProcessing.comp");

        //create the shadowmap texture
        shadowmapTexture = glGenTextures();
        shadowmapTextureUnit = Game.currentScene.GetNextTextureUnit();
        worldShader.SetInt("shadowmap", shadowmapTextureUnit);
        foliageShader.SetInt("shadowmap", shadowmapTextureUnit);
        glActiveTexture(GL_TEXTURE0 + shadowmapTextureUnit);
        glBindTexture(GL_TEXTURE_2D, shadowmapTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32F, Main.shadowmapSize, Main.shadowmapSize * 2, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (FloatBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float[] borderColor = { 1.0f, 1.0f, 1.0f, 1.0f };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);  
        
        //create/link the shadowmap frame buffer
        shadowmapFrameBuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, shadowmapFrameBuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowmapTexture, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        int samples = Main.multiSampleSamples;
        
        //create the FBO that is rendered to
        frameBuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        
        //create color buffer
        colorBufferTexture = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, colorBufferTexture);
        glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, GL_RGBA16F, Main.windowX, Main.windowY, true);
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D_MULTISAMPLE, colorBufferTexture, 0);
        
        //create the render buffer object
        renderBuffer = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer);
        glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, GL_DEPTH_COMPONENT32F, Main.windowX, Main.windowY);  
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, renderBuffer);
        
        //create rendered scene framebuffer/texture
        renderedSceneFramebuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, renderedSceneFramebuffer);
        renderedSceneTexture = glGenTextures();
        renderedSceneTextureUnit = Game.currentScene.GetNextTextureUnit();
        screenQuad.shader.SetInt("renderColor", renderedSceneTextureUnit);
        glActiveTexture(GL_TEXTURE0 + renderedSceneTextureUnit);
        glBindTexture(GL_TEXTURE_2D, renderedSceneTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, Main.windowX, Main.windowY, 0, GL_RGBA, GL_FLOAT, (FloatBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderedSceneTexture, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        //create the bloom mip textures
        bloomMipTextures = new int[BLOOM_MIP_LEVELS];
        bloomMipTextureUnits = new int[BLOOM_MIP_LEVELS];
        bloomMipSizes = new Vector2i[BLOOM_MIP_LEVELS];
        Vector2i bloomMipSize = new Vector2i(Main.windowX, Main.windowY);
        for(int i = 0; i < BLOOM_MIP_LEVELS; i++){
            bloomMipSize = bloomMipSize.div(2);
            bloomMipSizes[i] = new Vector2i(bloomMipSize);
            bloomMipTextures[i] = glGenTextures();
            bloomMipTextureUnits[i] = Game.currentScene.GetNextTextureUnit();
            glActiveTexture(GL_TEXTURE0 + bloomMipTextureUnits[i]);
            glBindTexture(GL_TEXTURE_2D, bloomMipTextures[i]);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, bloomMipSize.x, bloomMipSize.y, 0, GL_RGBA, GL_FLOAT, (FloatBuffer)null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        }
        bloomDownsampleShader.SetInt("outputImage", 0);
        bloomUpsampleShader.SetInt("outputImage", 0);
        bloomUpsampleShader.SetFloat("radius", BLOOM_RADIUS);
        bloomUpsampleShader.SetFloat("intensity", BLOOM_INTENSITY);

        //create the tree wind field texture
        foliageWindFieldTexture = glGenTextures();
        foliageWindFieldTextureUnit = Game.currentScene.GetNextTextureUnit();
        foliageShader.SetInt("windField", foliageWindFieldTextureUnit);
        foliageShadowmapShader.SetInt("windField", foliageWindFieldTextureUnit);
        glActiveTexture(GL_TEXTURE0 + foliageWindFieldTextureUnit);
        glBindTexture(GL_TEXTURE_2D, foliageWindFieldTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R16F, (int)Camera.WORLD_SIZE, (int)Camera.WORLD_SIZE, 0, GL_RED, GL_FLOAT, (FloatBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        foliageWindFieldOffsetTexture = glGenTextures();
        foliageWindFieldOffsetTextureUnit = Game.currentScene.GetNextTextureUnit();
        foliageWindFieldShader.SetInt("windOffsets", foliageWindFieldOffsetTextureUnit);
        glActiveTexture(GL_TEXTURE0 + foliageWindFieldOffsetTextureUnit);
        glBindTexture(GL_TEXTURE_2D, foliageWindFieldOffsetTexture);
        float[] foliageWindOffsets = new float[(int)Camera.WORLD_SIZE * (int)Camera.WORLD_SIZE];
        for(int i = 0; i < (int)Camera.WORLD_SIZE; i++){
            for(int j = 0; j < (int)Camera.WORLD_SIZE; j++){
                foliageWindOffsets[i * (int)Camera.WORLD_SIZE + j] = 0.085f * (float)((double)(i - j) + 40.0 * (PerlinNoise.Noise((double)(i) * Math.PI * 0.12, (double)(j) * Math.PI * 0.12)) - 0.5);
            }
        }
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R16F, (int)Camera.WORLD_SIZE, (int)Camera.WORLD_SIZE, 0, GL_RED, GL_FLOAT, foliageWindOffsets);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        //create the gaussian blur input textures
        blurDownsampleInputTexture = glGenTextures();
        blurDownsampleInputTextureUnit = Game.currentScene.GetNextTextureUnit();
        glActiveTexture(GL_TEXTURE0 + blurDownsampleInputTextureUnit);
        glBindTexture(GL_TEXTURE_2D, blurDownsampleInputTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, Main.windowX, Main.windowY, 0, GL_RGBA, GL_FLOAT, (FloatBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        blurHorizontalInputTexture = glGenTextures();
        blurHorizontalInputTextureUnit = Game.currentScene.GetNextTextureUnit();
        glActiveTexture(GL_TEXTURE0 + blurHorizontalInputTextureUnit);
        glBindTexture(GL_TEXTURE_2D, blurHorizontalInputTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, Main.windowX / 2, Main.windowY / 2, 0, GL_RGBA, GL_FLOAT, (FloatBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        blurVerticalInputTexture = glGenTextures();
        blurVerticalInputTextureUnit = Game.currentScene.GetNextTextureUnit();
        glActiveTexture(GL_TEXTURE0 + blurVerticalInputTextureUnit);
        glBindTexture(GL_TEXTURE_2D, blurVerticalInputTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, Main.windowX / 2, Main.windowY / 2, 0, GL_RGBA, GL_FLOAT, (FloatBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    }

    public void Update() {
        if(blurEnabled){
            blurAmount = (float)Math.min(1.0, Math.max(0.0, blurAmount + (blurAmount < blurTarget ? 1.0f : -1.0f) * Game.deltaTime * blurSpeed));
            if(blurAmount < 0.0001 && blurTarget < 0.0001) {
                blurEnabled = false;
                screenQuad.shader.SetBool("blurActive", false);
            }
        }
    }

    public void FixedUpdate() {
        //update the wind field
        glBindImageTexture(0, foliageWindFieldTexture, 0, false, 0, GL_WRITE_ONLY, GL_R16F);
        foliageWindFieldShader.Use();
        glDispatchCompute((int)Camera.WORLD_SIZE / 8, (int)Camera.WORLD_SIZE / 8, 1);
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
    }

    public void Draw() {
        if(Game.wireframe){
            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glViewport(0, 0, Main.windowX, Main.windowY);
            glDisable(GL_CULL_FACE);
            worldShader.Use();
            for(VoxelInstanceRenderer instanceRenderer : instanceRenderers.values()){
                instanceRenderer.Draw();
            }

            glDisable(GL_MULTISAMPLE);
            
            //blit together the multisample buffers into the post fx buffer
            glBindFramebuffer(GL_READ_FRAMEBUFFER, frameBuffer);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
            glBlitFramebuffer(0, 0, Main.windowX, Main.windowY, 0, 0, Main.windowX, Main.windowY, GL_COLOR_BUFFER_BIT, GL_NEAREST);
            
            return;  
        }

        glDisable(GL_MULTISAMPLE);

        //render to the shadow map from the sun's perspective
        glViewport(0, 0, Main.shadowmapSize, Main.shadowmapSize * 2);
        glCullFace(GL_FRONT);
        glBindFramebuffer(GL_FRAMEBUFFER, shadowmapFrameBuffer);
        glClear(GL_DEPTH_BUFFER_BIT);
        worldShadowmapShader.Use();
        for(Map.Entry<String, VoxelInstanceRenderer> instanceRenderer : instanceRenderers.entrySet()){
            if(variantFlags.get(instanceRenderer.getKey()).equals(RendererVariant.Standard)){
                instanceRenderer.getValue().DrawShadowPass();
            }
        }
        foliageShadowmapShader.Use();
        for(Map.Entry<String, VoxelInstanceRenderer> instanceRenderer : instanceRenderers.entrySet()){
            if(variantFlags.get(instanceRenderer.getKey()).equals(RendererVariant.Foliage)){
                instanceRenderer.getValue().DrawShadowPass();
            }
        }

        if(Main.MSAA_ENABLED) glEnable(GL_MULTISAMPLE);
        
        //bind the multisample buffers and render the scene to them
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, Main.windowX, Main.windowY);
        glCullFace(GL_BACK);
        worldShader.Use();
        for(Map.Entry<String, VoxelInstanceRenderer> instanceRenderer : instanceRenderers.entrySet()){
            if(variantFlags.get(instanceRenderer.getKey()).equals(RendererVariant.Standard)){
                instanceRenderer.getValue().Draw();
            }
        }
        foliageShader.Use();
        for(Map.Entry<String, VoxelInstanceRenderer> instanceRenderer : instanceRenderers.entrySet()){
            if(variantFlags.get(instanceRenderer.getKey()).equals(RendererVariant.Foliage)){
                instanceRenderer.getValue().Draw();
            }
        }

        glDisable(GL_MULTISAMPLE);
        
        //blit together the multisample buffers into the post fx buffer
        glBindFramebuffer(GL_READ_FRAMEBUFFER, frameBuffer);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, renderedSceneFramebuffer);
        glBlitFramebuffer(0, 0, Main.windowX, Main.windowY, 0, 0, Main.windowX, Main.windowY, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        
        //bloom pass
        if(BLOOM_ENABLED){
            //downsample iteratively
            int currentSrcTextureUnit = renderedSceneTextureUnit;
            bloomDownsampleShader.SetBool("isBaseMip", true);
            for(int i = 0; i < BLOOM_MIP_LEVELS; i++){
                glBindImageTexture(0, bloomMipTextures[i], 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);
                if(i == 1) bloomDownsampleShader.SetBool("isBaseMip", false);
                bloomDownsampleShader.SetInt("inputTexture", currentSrcTextureUnit);
                bloomDownsampleShader.SetVec2("outputSize", bloomMipSizes[i].x, bloomMipSizes[i].y);
                bloomDownsampleShader.Use();
                
                glDispatchCompute((int)Math.ceil(bloomMipSizes[i].x / 8.0), (int)Math.ceil(bloomMipSizes[i].y / 8.0), 1);
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
                currentSrcTextureUnit = bloomMipTextureUnits[i];
            }
            //upsample iteratively
            bloomUpsampleShader.SetBool("isBaseMip", false);
            for(int i = BLOOM_MIP_LEVELS - 1; i >= 1; i--){
                glBindImageTexture(0, bloomMipTextures[i - 1], 0, false, 0, GL_READ_WRITE, GL_RGBA16F);
                bloomUpsampleShader.SetInt("inputTexture", bloomMipTextureUnits[i]);
                bloomUpsampleShader.SetVec2("outputSize", bloomMipSizes[i - 1].x, bloomMipSizes[i - 1].y);
                bloomUpsampleShader.Use();
                glDispatchCompute((int)Math.ceil(bloomMipSizes[i - 1].x / 8.0), (int)Math.ceil(bloomMipSizes[i - 1].y / 8.0), 1);
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            }
            glBindImageTexture(0, renderedSceneTexture, 0, false, 0, GL_READ_WRITE, GL_RGBA16F);
            bloomUpsampleShader.SetBool("isBaseMip", true);
            bloomUpsampleShader.SetInt("inputTexture", bloomMipTextureUnits[0]);
            bloomUpsampleShader.SetVec2("outputSize", Main.windowX, Main.windowY);
            bloomUpsampleShader.Use();
            glDispatchCompute((int)Math.ceil(Main.windowX / 8.0), (int)Math.ceil(Main.windowY / 8.0), 1);
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }
        
        //post effect pass (tonemapping)
        if(blurEnabled){
            //copy the tonemapped result to the blur input for the blur pass (copying so we can blend the blur result)
            glBindImageTexture(0, renderedSceneTexture, 0, false, 0, GL_READ_ONLY, GL_RGBA16F);
            glBindImageTexture(1, blurDownsampleInputTexture, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);
            postProcessingShader.Use();
            glDispatchCompute((int)Math.ceil(Main.windowX / 8.0), (int)Math.ceil(Main.windowY / 8.0), 1);
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }
        glBindImageTexture(0, renderedSceneTexture, 0, false, 0, GL_READ_ONLY, GL_RGBA16F);
        glBindImageTexture(1, renderedSceneTexture, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);
        postProcessingShader.Use();
        glDispatchCompute((int)Math.ceil(Main.windowX / 8.0), (int)Math.ceil(Main.windowY / 8.0), 1);
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        
        //blur pass
        if(blurEnabled){
            screenQuad.shader.SetFloat("blurAmount", 0.5f * (float)Math.sin(blurAmount * Math.PI - 0.5 * Math.PI) + 0.5f);
            screenQuad.shader.SetInt("blurColor", blurHorizontalInputTextureUnit);
            gaussianBlurShader.SetInt("inputTexture", blurDownsampleInputTextureUnit);
            for(int i = 0; i < BLUR_PASSES; i++){
                //horizontal pass
                glBindImageTexture(0, blurVerticalInputTexture, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);
                gaussianBlurShader.SetVec2("direction", 1.0f, 0.0f);
                gaussianBlurShader.Use();
                glDispatchCompute((int)Math.ceil(Main.windowX / 16.0), (int)Math.ceil(Main.windowY / 16.0), 1);
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
                
                //vertical pass
                glBindImageTexture(0, blurHorizontalInputTexture, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);
                gaussianBlurShader.SetInt("inputTexture", blurVerticalInputTextureUnit);
                gaussianBlurShader.SetVec2("direction", 0.0f, 1.0f);
                gaussianBlurShader.Use();
                glDispatchCompute((int)Math.ceil(Main.windowX / 16.0), (int)Math.ceil(Main.windowY / 16.0), 1);
                glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

                if(i < BLUR_PASSES - 1) gaussianBlurShader.SetInt("inputTexture", blurHorizontalInputTextureUnit);
            }
        }

        int currentSrcTextureUnit = renderedSceneTextureUnit;
        if(Input.GetKeyDown(Input.KEY_KP_1)) currentSrcTextureUnit = bloomMipTextureUnits[0];
        else if(Input.GetKeyDown(Input.KEY_KP_2)) currentSrcTextureUnit = shadowmapTextureUnit;
        else if(Input.GetKeyDown(Input.KEY_KP_3)) currentSrcTextureUnit = foliageWindFieldTextureUnit;
        screenQuad.shader.SetInt("renderColor", currentSrcTextureUnit);
        
        //render to quad with vignette and gamma correction
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        screenQuad.shader.Use();
        glBindVertexArray(screenQuad.VAO);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, (long)0);

        if(Main.MSAA_ENABLED) glEnable(GL_MULTISAMPLE);
    }

    /**
     * Gets all voxel mesh objects that are loaded and creates buffers for each of them.
     * Should be called once at scene open in a standard implementation.
     * TODO: either remove or add an instance/non-instanced flag, so that non-instanced meshes do not 
     * have instance buffers created needlessly.
     */
    public static void LoadAllMeshes(){
        instance.instanceRenderers = new HashMap<>();
        for(VoxelData mesh : VoxelLoader.meshObjects.values()){
            instance.instanceRenderers.put(mesh.name, new VoxelInstanceRenderer(mesh.name));
        }

        //set the variant flags
        instance.variantFlags = new HashMap<>();
        for(VoxelData mesh : VoxelLoader.meshObjects.values()){
            RendererVariant variant = RendererVariant.Standard;
            for(String cmp : FOLIAGE_NAMES){
                if(mesh.name.equals(cmp)){
                    variant = RendererVariant.Foliage;
                    break;
                }
            }
            instance.variantFlags.put(mesh.name, variant);
        }
    }

    /**
     * Manually add a mesh to be instance rendered
     * @param mesh
     *      - the loaded voxel mesh.
     */
    public static void LoadInstancedMesh(VoxelData mesh){
        instance.instanceRenderers.put(mesh.name, new VoxelInstanceRenderer(mesh));

        //set the variant flag
        RendererVariant variant = RendererVariant.Standard;
        for(String cmp : FOLIAGE_NAMES){
            if(mesh.name.equals(cmp)){
                variant = RendererVariant.Foliage;
                break;
            }
        }
        instance.variantFlags.put(mesh.name, variant);
    }

    /**
     * Adds a mesh instance. Call UpdateMeshData() afterwards to update the instance buffers.
     * @param x
     *      - local x-coordinate
     * @param z
     *      - local z-cordinate
     */
    public static void AddMeshInstance(String name, int x, int z){
        instance.instanceRenderers.get(name).AddInstance(x, z);
    }

    /**
     * Removes a mesh instance. Call UpdateMeshData() afterwards to update the instance buffers.
     * @param x
     *      - local x-coordinate
     * @param z
     *      - local z-cordinate
     */
    public static void RemoveInstance(String name, int x, int z){
        instance.instanceRenderers.get(name).RemoveInstance(x, z);
    }

    /**
     * Updates the GPU instanced attributes for the meshes types that need it. Call this whenever instances are added.
     */
    public static void UpdateInstancedData(){
        for(VoxelInstanceRenderer mesh : instance.instanceRenderers.values()){
            mesh.UpdateInstanceData();
        }
    }

    /**
     * Blur the scene, interpolated in.
     * @param enableTime
     *      - the duration, in seconds to interpolate the blur in. A value of 1.0f will take one second.
     */
    public static void EnableBlur(float enableTime){
        instance.blurEnabled = true;
        instance.blurTarget = 1.0f;
        instance.blurSpeed = 1.0f / enableTime; 
        instance.screenQuad.shader.SetBool("blurActive", true);
    }
    
    /**
     * Unblur the scene, interpolated out.
     * @param enableTime
     *      - the duration, in seconds to interpolate the blur out. A value of 1.0f will take one second.
     */
    public static void DisableBlur(float disableTime){
        instance.blurEnabled = true;
        instance.blurTarget = 0.0f;
        instance.blurSpeed = 1.0f / disableTime; 
        instance.screenQuad.shader.SetBool("blurActive", true);
    }

    public void OnWindowResize() {
        int samples = Main.multiSampleSamples;

        //update color buffer texture
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        glActiveTexture(GL_TEXTURE0 + colorBufferTextureUnit);
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, colorBufferTexture);
        glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, GL_RGBA16F, Main.windowX, Main.windowY, true);
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);

        //update the render buffer object
        glBindRenderbuffer(GL_RENDERBUFFER, renderBuffer);
        glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, GL_DEPTH24_STENCIL8, Main.windowX, Main.windowY);  
        glBindRenderbuffer(GL_RENDERBUFFER, 0);

        //update rendered scene buffer texture
        glBindFramebuffer(GL_FRAMEBUFFER, renderedSceneFramebuffer);
        glActiveTexture(GL_TEXTURE0 + renderedSceneTextureUnit);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, Main.windowX, Main.windowY, 0, GL_RGBA, GL_FLOAT, (FloatBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        //update the bloom mip textures
        Vector2i bloomMipSize = new Vector2i(Main.windowX, Main.windowY);
        for(int i = 0; i < BLOOM_MIP_LEVELS; i++){
            bloomMipSize = bloomMipSize.div(2);
            bloomMipSizes[i] = new Vector2i(bloomMipSize);
            glActiveTexture(GL_TEXTURE0 + bloomMipTextureUnits[i]);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, bloomMipSize.x, bloomMipSize.y, 0, GL_RGBA, GL_FLOAT, (FloatBuffer)null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        }

        //update the gaussian blur input textures
        glActiveTexture(GL_TEXTURE0 + blurDownsampleInputTextureUnit);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, Main.windowX, Main.windowY, 0, GL_RGBA, GL_FLOAT, (FloatBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glActiveTexture(GL_TEXTURE0 + blurHorizontalInputTextureUnit);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, Main.windowX / 2, Main.windowY / 2, 0, GL_RGBA, GL_FLOAT, (FloatBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glActiveTexture(GL_TEXTURE0 + blurVerticalInputTextureUnit);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, Main.windowX / 2, Main.windowY / 2, 0, GL_RGBA, GL_FLOAT, (FloatBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    }

    private class ScreenQuad{
        public int VAO;
        public Shader shader;

        public ScreenQuad(Shader shader){
            this.shader = shader;

            VAO = glGenVertexArrays();
            glBindVertexArray(VAO);

            int EBO = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, new int[] {0, 1, 2, 2, 1, 3}, GL_STATIC_DRAW);

            int VBO = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, VBO);
            glVertexAttribIPointer(0, 1, GL_INT, 0, 0);
            glEnableVertexAttribArray(0);
            glBufferData(GL_ARRAY_BUFFER, new int[] {0, 1, 2, 3}, GL_STATIC_DRAW);
        }
    }
}