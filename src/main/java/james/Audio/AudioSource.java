package james.Audio;

import org.lwjgl.openal.AL10;

public class AudioSource {
    
    private int sourceID;

    public AudioSource(){
        sourceID = AL10.alGenSources();
        AL10.alSourcef(sourceID, AL10.AL_GAIN, 1.0f);
        AL10.alSourcef(sourceID, AL10.AL_PITCH, 1.0f);
        AL10.alSource3f(sourceID, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
    }
    public AudioSource(float radius, float rolloffFactor, float maxDistance){
        sourceID = AL10.alGenSources();
        AL10.alSourcef(sourceID, AL10.AL_GAIN, 1.0f);
        AL10.alSourcef(sourceID, AL10.AL_PITCH, 1.0f);
        AL10.alSource3f(sourceID, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);

        AL10.alSourcef(sourceID, AL10.AL_REFERENCE_DISTANCE, radius);
        AL10.alSourcef(sourceID, AL10.AL_ROLLOFF_FACTOR, rolloffFactor);
        AL10.alSourcef(sourceID, AL10.AL_MAX_DISTANCE, maxDistance);
    }

    /**
     * Play the given sound from the current audio source.
     * @param name
     *      - the name of the sound, including its file extension.
     */
    public void Play(String name){
        AL10.alSourceStop(sourceID);
        AL10.alSourcei(sourceID, AL10.AL_BUFFER, AudioManager.instance.sounds.get(name));
        AL10.alSourcePlay(sourceID);
    }
    
    /**
     * Play the given sound from the current audio source.
     * @param buffer
     *      - the sound's buffer value.
     */
    public void Play(int buffer){
        AL10.alSourceStop(sourceID);
        AL10.alSourcei(sourceID, AL10.AL_BUFFER, buffer);
        AL10.alSourcePlay(sourceID);
    }

    /**
     * Pause the current audio source.
     */
    public void Pause(){
        AL10.alSourcePause(sourceID);
    }

    /**
     * Unpause the current audio source.
     */
    public void UnPause(){
        AL10.alSourcePlay(sourceID);
    }

    /**
     * Stop the current audio source.
     */
    public void Stop(){
        AL10.alSourceStop(sourceID);
    }
    
    /**
     * Set the audio source's gain.
     * @param volume
     *      - any floating point number. Default value is 1.0f.
     */
    public void SetVolume(float volume){
        AL10.alSourcef(sourceID, AL10.AL_GAIN, volume);
    }

    /**
     * Set the audio source's pitch.
     * @param pitch
     *      - any floating point number. Default value is 1.0f.
     */
    public void SetPitch(float pitch){
        AL10.alSourcef(sourceID, AL10.AL_PITCH, pitch);
    }

    /**
     * Set the audio source's position, in world coordinates.
     * @param x
     * @param y
     * @param z
     */
    public void SetPosition(float x, float y, float z){
        AL10.alSource3f(sourceID, AL10.AL_POSITION, x, y, z);
    }

    /**
     * Set the audio source's velocity, in world coordinates.
     * @param x
     * @param y
     * @param z
     */
    public void SetVelocity(float x, float y, float z){
        AL10.alSource3f(sourceID, AL10.AL_VELOCITY, x, y, z);
    }

    /**
     * Set whether the audio source is to loop it's clip when played.
     * @param loop
     */
    public void SetLooping(boolean loop){
        AL10.alSourcei(sourceID, AL10.AL_LOOPING, loop ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    /**
     * Returns true if the audio source is currently playing an audio clip.
     * @return
     */
    public boolean IsPlaying() {
        return AL10.alGetSourcei(sourceID, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    /**
     * Clean up the audio source.
     */
    public void Free(){
        AL10.alDeleteSources(sourceID);
    }
}
