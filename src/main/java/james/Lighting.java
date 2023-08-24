package james;

import static org.lwjgl.opengl.GL44C.*;

import java.util.ArrayList;
import java.util.Random;

import org.joml.Vector2f;
import org.joml.Vector3f;

import james.GameScene.GameState;

public class Lighting implements GameObject{
    public static Lighting instance;

    private int lightingDataUBO;
    private int shadowmapDataUBO;
    private int worldDataUBO;
    public Vector3f sunColor;
    public Vector3f moonColor;
    public float lightColorSaturation;
    public float lightExposure;
    private EaseCurve lightColorSaturationCurve;
    private EaseCurve shadowOffsetCurve;
    private EaseCurve shadowBackLeftOffsetCurve;
    private EaseCurve shadowStrengthCurve;
    private EaseCurve shadowBackLeftStrengthCurve;

    public Vector3f sunLightDirection;
    public Vector3f moonLightDirection;
    public Vector3f sunShadowDirection;
    public Vector3f moonShadowDirection;
    public Vector3f activeShadowDirection;
    public float activeShadowOffset;
    public float activeShadowBackLeftOffset;
    public float activeShadowStrength;
    public float activeShadowBackLeftStrength;

    public float activeShadowRotation;
    
    /**
     * In range [0, 1]
     */
    public float dayNightCycle;

    /**
     * The current time, in days.
     */
    public float gameTime;


    private static final float NIGHT_END = 0.97f;
    private static final float SUNRISE_START = 0.98f;
    private static final float SUNRISE_END = 0.9925f;
    private static final float SUN_PEAK = 0.245f;
    private static final float DAY_END = 0.505f;
    private static final float MOONRISE_START = 0.515f;
    private static final float MOONRISE_END = 0.5205f;
    private static final float MOON_PEAK = 0.747f;
    
    private static final int SHADOW_KERNEL_MAX_SHELLS = 8;
    private static final int SHADOW_POISSON_MAX_SAMPLES = 7;
    //private static final float SHADOW_KERNEL_SHELL_SIZE = 0.0f;
    private static final float SHADOW_KERNEL_SHELL_SIZE = 0.3141f;

    public Lighting(){
        Game.AddObject(this);
        instance = this;

        lightExposure = 0.67f;
        sunLightDirection = new Vector3f(0.0f);
        sunLightDirection = new Vector3f(0.0f);
        sunShadowDirection = new Vector3f(0.0f);
        moonShadowDirection = new Vector3f(0.0f);
        activeShadowDirection = new Vector3f(0.0f);
        sunColor = new Vector3f(1.0f, 0.952f, 0.788f);
        moonColor = new Vector3f(0.701f, 0.831f, 1.0f);

        lightColorSaturationCurve = new EaseCurve();
        //lightColorSaturationCurve.AddPoint(0.12f, 1.0f, EaseType.Linear);
        lightColorSaturationCurve.AddPoint(SUN_PEAK - 0.1f, 1.0f, EaseType.Linear);
        lightColorSaturationCurve.AddPoint(SUN_PEAK - 0.025f, 0.65f, EaseType.Linear);
        lightColorSaturationCurve.AddPoint(SUN_PEAK, 0.4f, EaseType.QuadraticOut);
        lightColorSaturationCurve.AddPoint(SUN_PEAK + 0.025f, 0.65f, EaseType.QuadraticIn);
        lightColorSaturationCurve.AddPoint(SUN_PEAK + 0.1f, 1.0f, EaseType.Linear);
        lightColorSaturationCurve.AddPoint(0.4f, 1.0f, EaseType.Linear);
        lightColorSaturationCurve.AddPoint(0.515f, 1.2f, EaseType.QuadraticInOut);
        lightColorSaturationCurve.AddPoint(0.52f, 1.0f, EaseType.QuadraticInOut);
        //lightColorSaturationCurve.AddPoint(0.37f, 1.0f, EaseType.Linear);

        shadowOffsetCurve = new EaseCurve();
        shadowOffsetCurve.AddPoint(NIGHT_END, 0.0f, EaseType.Linear);
        shadowOffsetCurve.AddPoint(SUNRISE_START, 1.0f, EaseType.SineInOut);
        shadowOffsetCurve.AddPoint(SUNRISE_END, 0.0f, EaseType.SineInOut);
        shadowOffsetCurve.AddPoint(DAY_END, 0.0f, EaseType.Linear);
        shadowOffsetCurve.AddPoint(MOONRISE_START, 1.0f, EaseType.SineInOut);
        shadowOffsetCurve.AddPoint(MOONRISE_END, 0.0f, EaseType.SineInOut);
        
        shadowStrengthCurve = new EaseCurve();
        shadowStrengthCurve.AddPoint(NIGHT_END, 0.6f, EaseType.Linear);
        shadowStrengthCurve.AddPoint(SUNRISE_START, 0.3f, EaseType.SineInOut);
        shadowStrengthCurve.AddPoint(SUNRISE_END, 0.5f, EaseType.SineInOut);
        shadowStrengthCurve.AddPoint(0.1f, 0.8f, EaseType.QuadraticOut);
        shadowStrengthCurve.AddPoint(0.4f, 0.8f, EaseType.Linear);
        shadowStrengthCurve.AddPoint(DAY_END, 0.5f, EaseType.Linear);
        shadowStrengthCurve.AddPoint(MOONRISE_START, 0.5f, EaseType.SineInOut);
        shadowStrengthCurve.AddPoint(MOONRISE_END, 0.5f, EaseType.SineInOut);
        
        float sunPeakOffsetRadius = 0.05f;
        float moonPeakOffsetRadius = 0.08f;
        shadowBackLeftOffsetCurve = new EaseCurve();
        shadowBackLeftOffsetCurve.AddPoint(SUN_PEAK - sunPeakOffsetRadius, 0.0f, EaseType.Linear);
        shadowBackLeftOffsetCurve.AddPoint(SUN_PEAK, 1.0f, EaseType.SineInOut);
        shadowBackLeftOffsetCurve.AddPoint(SUN_PEAK + sunPeakOffsetRadius, 0.0f, EaseType.SineInOut);
        shadowBackLeftOffsetCurve.AddPoint(MOON_PEAK - moonPeakOffsetRadius, 0.0f, EaseType.Linear);
        shadowBackLeftOffsetCurve.AddPoint(MOON_PEAK, 1.0f, EaseType.SineInOut);
        shadowBackLeftOffsetCurve.AddPoint(MOON_PEAK + moonPeakOffsetRadius, 0.0f, EaseType.SineInOut);

        sunPeakOffsetRadius *= 1.5f; moonPeakOffsetRadius *= 1.75f;
        shadowBackLeftStrengthCurve = new EaseCurve();
        shadowBackLeftStrengthCurve.AddPoint(SUN_PEAK - sunPeakOffsetRadius, 1.0f, EaseType.Linear);
        shadowBackLeftStrengthCurve.AddPoint(SUN_PEAK, 0.45f, EaseType.SineInOut);
        shadowBackLeftStrengthCurve.AddPoint(SUN_PEAK + sunPeakOffsetRadius, 1.0f, EaseType.SineInOut);
        shadowBackLeftStrengthCurve.AddPoint(MOON_PEAK - moonPeakOffsetRadius, 1.0f, EaseType.Linear);
        shadowBackLeftStrengthCurve.AddPoint(MOON_PEAK, 0.35f, EaseType.SineInOut);
        shadowBackLeftStrengthCurve.AddPoint(MOON_PEAK + moonPeakOffsetRadius, 1.0f, EaseType.SineInOut);

        //create global buffer for lighting info
        lightingDataUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, lightingDataUBO);
        glBufferData(GL_UNIFORM_BUFFER, (long)108, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_UNIFORM_BUFFER, 4, lightingDataUBO);

        //create global buffer for world data
        worldDataUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, worldDataUBO);
        glBufferData(GL_UNIFORM_BUFFER, (long)8, GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_UNIFORM_BUFFER, 6, worldDataUBO);

        //create and assign global buffer for shadowmap data
        int poissonTotalSampleCount = SHADOW_KERNEL_MAX_SHELLS * SHADOW_POISSON_MAX_SAMPLES;
        shadowmapDataUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, shadowmapDataUBO);
        glBufferData(GL_UNIFORM_BUFFER, (long)(4 * (poissonTotalSampleCount * 2 + 4)), GL_DYNAMIC_DRAW);
        glBindBufferBase(GL_UNIFORM_BUFFER, 5, shadowmapDataUBO);

        float[] shadowmapDataBuffer = new float[poissonTotalSampleCount * 2 + 4];
        shadowmapDataBuffer[0] = Float.intBitsToFloat(SHADOW_KERNEL_MAX_SHELLS);
        shadowmapDataBuffer[1] = Float.intBitsToFloat(SHADOW_POISSON_MAX_SAMPLES);
        shadowmapDataBuffer[2] = SHADOW_KERNEL_SHELL_SIZE;
        Random random = new Random();
        int curIndex = 4;
        for(int i = 0; i < SHADOW_KERNEL_MAX_SHELLS; i++){
            float rotOffset = random.nextFloat() * 0.5f * (float)Math.PI;
            for(int j = 0; j < SHADOW_POISSON_MAX_SAMPLES; j++){
                float rotation = rotOffset + (float)Math.PI * 2.0f * (float)j / (float)SHADOW_POISSON_MAX_SAMPLES;
                shadowmapDataBuffer[curIndex] = (float)Math.cos(rotation);
                shadowmapDataBuffer[curIndex + 1] = (float)Math.sin(rotation);
                curIndex += 2;
            }
        }
        //for(int i = 4; i < shadowmapDataBuffer.length; i++) System.out.println(shadowmapDataBuffer[i]);
        glBindBuffer(GL_UNIFORM_BUFFER, shadowmapDataUBO);
        glBufferSubData(GL_UNIFORM_BUFFER, (long)0, shadowmapDataBuffer);
    }

    public void Update() {
        if(GameScene.state == GameState.Paused) {
            //
        }
        else if(Input.GetKeyDown(Input.KEY_1)) {
            //
        }
        else if(Input.GetKeyDown(Input.KEY_2)) {
            gameTime += Game.deltaTime * (1.0f / 60.0f) / 4.0f;
        }
        else if(Input.GetKeyDown(Input.KEY_3)) {
            gameTime += Game.deltaTime * (1.0f / 60.0f) / 2.0f;
        }
        else if(Input.GetKeyDown(Input.KEY_4)) {
            gameTime += Game.deltaTime * (1.0f / 60.0f) * 2.0f;
        }
        else if(Input.GetKeyDown(Input.KEY_5)) {
            gameTime += Game.deltaTime * (1.0f / 60.0f) * 18.0f;
        }
        else {
            gameTime += Game.deltaTime * (1.0f / 60.0f) / 6.0f;
        }
        dayNightCycle = gameTime % 1.0f;
        
        //calculate light rotations/directions for diffuse lighting in fragment shader
        //https://www.desmos.com/calculator/st0ezxmtn2
        float sunlightRotation = (float)Math.PI * 2.0f * gameTime;
        Vector3f lightOffsetBias = new Vector3f(-0.1f, 0.0f, -0.1f);
        
        //sunlight direction
        float sunlightOffset = -1.4f;
        float sunlightFreq = 0.95f;
        float adjSunlightRotation = (sunlightRotation + 0.5f * (float)Math.PI) % (2.0f * (float)Math.PI);

        double sunlightXZ = Math.cos(adjSunlightRotation * sunlightFreq + sunlightOffset);
        double sunlightY = Math.sin(adjSunlightRotation * sunlightFreq + sunlightOffset);
        sunLightDirection = new Vector3f((float)sunlightXZ, (float)sunlightY, -(float)sunlightXZ).normalize().add(lightOffsetBias).normalize();   

        //moonlight direction
        float moonlightOffset = -0.25f;
        float moonlightFreq = 0.65f;
        float adjMoonlightRotation = (sunlightRotation + 1.5f * (float)Math.PI) % (2.0f * (float)Math.PI);
        
        double moonlightXZ = Math.cos(adjMoonlightRotation * moonlightFreq + moonlightOffset);
        double moonlightY = Math.sin(adjMoonlightRotation * moonlightFreq + moonlightOffset);
        moonLightDirection = new Vector3f((float)moonlightXZ, (float)moonlightY, -(float)moonlightXZ).normalize().add(lightOffsetBias).normalize();


        //calculate sun/moon rotations/directions for shadow mapping
        //https://www.desmos.com/calculator/st0ezxmtn2
        Vector3f shadowOffsetBias = new Vector3f(0.0f);

        //sun direction
        float sunOffset = -1.23f;
        float sunFreq = 0.9f;
        float adjSunRotation = (sunlightRotation + 0.5f * (float)Math.PI) % (2.0f * (float)Math.PI);

        double sunXZ = Math.cos(adjSunRotation * sunFreq + sunOffset);
        double sunY = Math.sin(adjSunRotation * sunFreq + sunOffset);
        sunShadowDirection = new Vector3f((float)sunXZ, (float)sunY, -(float)sunXZ).normalize().add(shadowOffsetBias).normalize();   

        //moon direction
        float moonOffset = 0.32f;
        float moonFreq = 0.4f;
        float adjMoonRotation = (sunlightRotation + 1.5f * (float)Math.PI) % (2.0f * (float)Math.PI);
        
        double moonXZ = Math.cos(adjMoonRotation * moonFreq + moonOffset);
        double moonY = Math.sin(adjMoonRotation * moonFreq + moonOffset);
        moonShadowDirection = new Vector3f((float)moonXZ, (float)moonY, -(float)moonXZ).normalize().add(shadowOffsetBias).normalize();

        
        //set the day/night cycle's associated shadow strength/offset parameters
        boolean daytime = InRange(dayNightCycle, SUNRISE_START, MOONRISE_START);

        lightColorSaturation = lightColorSaturationCurve.Evaluate(dayNightCycle);
        activeShadowOffset = shadowOffsetCurve.Evaluate(dayNightCycle);
        activeShadowBackLeftOffset = shadowBackLeftOffsetCurve.Evaluate(dayNightCycle);
        activeShadowStrength = shadowStrengthCurve.Evaluate(dayNightCycle);
        activeShadowBackLeftStrength = shadowBackLeftStrengthCurve.Evaluate(dayNightCycle);
        
        if(daytime){
            activeShadowRotation = adjSunRotation * sunFreq + sunOffset;
            activeShadowDirection = new Vector3f(sunShadowDirection).normalize();
        }
        else{
            activeShadowRotation = adjMoonRotation * moonFreq + moonOffset;
            activeShadowDirection = new Vector3f(moonShadowDirection).normalize();
        }

        float[] lightingInfo = new float[] {
            sunLightDirection.x, sunLightDirection.y, sunLightDirection.z, 0.0f,
            moonLightDirection.x, moonLightDirection.y, moonLightDirection.z, 0.0f,
            activeShadowDirection.x, activeShadowDirection.y, activeShadowDirection.z, 0.0f,
            sunColor.x, sunColor.y, sunColor.z, 0.0f,
            moonColor.x, moonColor.y, moonColor.z,
            dayNightCycle,
            activeShadowStrength,
            activeShadowBackLeftStrength,
            activeShadowOffset,
            activeShadowBackLeftOffset,
            lightExposure,
            lightColorSaturation
            };
        glBindBuffer(GL_UNIFORM_BUFFER, lightingDataUBO);
        glBufferSubData(GL_UNIFORM_BUFFER, (long)0, lightingInfo);

        //update the world data UBO
        glBindBuffer(GL_UNIFORM_BUFFER, worldDataUBO);
        glBufferSubData(GL_UNIFORM_BUFFER, (long)0, new float[]{ (float)Game.time, gameTime });
    }

    public void FixedUpdate() {
    }

    public void Draw() {
    }


    public static enum EaseType { Linear, SineInOut, SineIn, SineOut, QuadraticInOut, QuadraticIn, QuadraticOut };
    private class EaseCurve{
        private ArrayList<Vector2f> points;
        private ArrayList<EaseType> easeTypes;

        public EaseCurve() {
            points = new ArrayList<>();
            easeTypes = new ArrayList<>();
        }

        /**
         * Adds a control point to the easing curve. Will ease to y when t = x. <p>
         * Ease types: symmetric with https://easings.net/
         * @param x
         *      - in range [0, 1].
         * @param y
         *      - any floating point number.
         * @param easeType
         *      - the ease in function to use when easing into this point from the previous.
         *  may be NULL if the point is located at x = 0.0.
         */
        public void AddPoint(float x, float y, EaseType easeTypeIn){

            int i = 0;
            while(i < points.size()){
                if(x >
                 points.get(i).x){
                    i++;
                    continue;
                }
                else if(x == points.get(i).x){
                    points.remove(i);
                    easeTypes.remove(i);
                    break;
                }
                else break;
            }
            points.add(i, new Vector2f(x, y));
            easeTypes.add(i, easeTypeIn);
        }

        public float Evaluate(float t){
            if(points.size() == 0) return 0.0f;
            else if(points.size() == 1) return points.get(0).y;

            Vector2f from;
            Vector2f to;
            if(t > points.get(points.size() - 1).x){
                from = points.get(points.size() - 1);
                to = points.get(0);
                return from.y + (to.y - from.y) * Ease((t - from.x) / (to.x + 1.0f - from.x), easeTypes.get(0));
            }
            if(t < points.get(0).x){
                from = points.get(points.size() - 1);
                to = points.get(0);
                return from.y + (to.y - from.y) * Ease((t + 1.0f - from.x) / (to.x + 1.0f - from.x), easeTypes.get(0));
            }
            for(int i = 1; i < points.size(); i++) {
                to = points.get(i);
                if(t < to.x){
                    from = points.get(i - 1);
                    return from.y + (to.y - from.y) * Ease((t - from.x) / (to.x - from.x), easeTypes.get(i));
                }
            }
            return 0.0f;
        }

        private float Ease(float t, EaseType easeType){
            if(easeType == EaseType.SineInOut){
                return 0.5f * (float)Math.sin(Math.PI * t - 0.5f * Math.PI) + 0.5f;
            }
            else if(easeType == EaseType.SineIn){
                return (float)Math.sin(0.5f * Math.PI * t - 0.5f * Math.PI) + 1.0f;
            }
            else if(easeType == EaseType.SineOut){
                return (float)Math.sin(0.5f * Math.PI * t);
            }
            else if(easeType == EaseType.QuadraticInOut){
                return t < 0.5f ? 2.0f * (float)Math.pow(t, 2.0) : 1.0f - 0.5f * (float)Math.pow(-2.0 * t + 2.0, 2.0);
            }
            else if(easeType == EaseType.QuadraticIn){
                return (float)Math.pow(t, 2.0);
            }
            else if(easeType == EaseType.QuadraticOut){
                return 1.0f - (float)Math.pow(1.0 - t, 2.0);
            }
            else if(easeType == EaseType.Linear){
                return t;
            }
            return 0.0f;
        }
    }

    /**
     * Membership checker for range [l, h] as a subset of [0, 1]. <p>
     * If l > h, it wraps the range as
     * [l, h] / ~ where x ~ y <=> x = y OR x, y /in {0, 1}.
     * @param input
     * @param l
     *      - in range [0, 1].
     * @param h
     *      - in range [0, 1].
     */
    private static boolean InRange(float input, float l, float h){
        if(l < 0.0f || l > 1.0f | h < 0.0f || h > 1.0f) throw new Error("invalid low-high values. must be in range [0, 1]");
        if(l > h){
            return input >= l || input <= h;
        }
        return input <= h && input >= l;
    }
}
