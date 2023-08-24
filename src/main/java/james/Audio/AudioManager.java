package james.Audio;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;

import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;
//import org.lwjgl.openal.ALCapabilities;

import james.Main;

public class AudioManager {
    private final int SOURCEPOOL_INTERFACE_SIZE_DEFAULT = 10;
    private final int SOURCEPOOL_INTERFACE_SIZE_MAXIMUM = 20;
    private final boolean DEBUG_SOURCEPOOLS = false;

    public static AudioManager instance;
    public HashMap<String, Integer> sounds;

    private long device;
    private long context;
    private ALCCapabilities alcCapabilities;
    private ArrayList<Integer> buffers;
    
    private Deque<AudioSource> interfaceSourcePool;

    /**
     * Initialize the audio manager. Should call once at program start in a standard implementation.
     */
    public AudioManager(){
        instance = this;

        String defDeviceName = ALC10.alcGetString(0, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);
        device = ALC10.alcOpenDevice(defDeviceName);
        alcCapabilities = ALC.createCapabilities(device);

        context = ALC10.alcCreateContext(device, (IntBuffer)null);
        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(alcCapabilities);

        sounds = new HashMap<>();
        buffers = new ArrayList<>();

        //set distance model
        AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE_CLAMPED);

        LoadSounds();

        interfaceSourcePool = new LinkedList<>();
        for(int i = 0; i < SOURCEPOOL_INTERFACE_SIZE_DEFAULT; i++){
            interfaceSourcePool.push(new AudioSource(0.0f, 0.0f, 0.0f));
        }
    }

    public void Free(){
        ALC10.alcMakeContextCurrent(MemoryUtil.NULL);
        ALC10.alcDestroyContext(instance.context);
        ALC10.alcCloseDevice(instance.device);
    }

    /**
     * Load all the sounds that are to be kept in memory for the duration of the program.
     */
    private void LoadSounds(){
        //get all files ending with .wav extension, load  noted files
        File directory = new File(new File("").getAbsolutePath() + Main.RESOURCE_PATH + "audio");
        if (directory.isDirectory()) {
            File[] fileList = directory.listFiles((dir, name) -> name.endsWith(".wav"));
            if (fileList != null) {
                for (File file : fileList) {
                    if (file.isFile()) {
                        LoadSound(file.getName());
                    }
                }
            }
        }
    }

    public int LoadSound(String name){
        if(sounds.containsKey(name)) return sounds.get(name);
        
        int buffer = AL10.alGenBuffers();
        buffers.add(buffer);
        WaveData waveFile = WaveData.create(name);
        AL10.alBufferData(buffer, waveFile.format, waveFile.data, waveFile.samplerate);
        waveFile.dispose();

        sounds.put(name, buffer);
        return buffer;
    }

    public void SetListenerData(Vector3f position, Vector3f velocity){
        AL10.alListener3f(AL10.AL_POSITION, position.x, position.y, position.z);
        AL10.alListener3f(AL10.AL_VELOCITY, velocity.x, velocity.y, velocity.z);
    }

    public void PlayInterfaceSound(String name, float volume, float pitch){
        if(interfaceSourcePool.peekFirst().IsPlaying()){
            if(interfaceSourcePool.size() < SOURCEPOOL_INTERFACE_SIZE_MAXIMUM){
                //add new source to pool.
                if(DEBUG_SOURCEPOOLS) System.out.println("added source to interface source pool");
                interfaceSourcePool.push(new AudioSource(0.0f, 0.0f, 0.0f));
            }
            else{
                if(DEBUG_SOURCEPOOLS) System.out.println("interface source pool reached maximum size. audio clipping will occur.");
                interfaceSourcePool.peekFirst().Stop();
            }
        }
        AudioSource source = interfaceSourcePool.pollFirst();
        source.SetVolume(volume);
        source.SetPitch(pitch);
        source.Play(name);
        interfaceSourcePool.addLast(source);
    }
}