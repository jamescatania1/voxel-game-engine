package james.Audio;

import org.joml.Vector3f;

public class AudioLibrary {
    
    /**
     * Clean up the Audio context.
     */
    public static void Free(){
        AudioManager.instance.Free();
    }

    /**
     * Loads sound file that is located in the resources/sounds folder. Returns the file's buffer.
     * @param name
     *      - name of the file, including its file extension.
     * @return
     *      - the sound's buffer.
     */
    public static int LoadSound(String name){
        return AudioManager.instance.LoadSound(name);
    }

    /**
     * Set the position/velocity info of the 3D sound listener.
     * @param position
     *      - the listener's position, in world space.
     * @param velocity
     *      - the listener's velocity, in world space.
     */
    public static void SetListenerData(Vector3f position, Vector3f velocity){
        AudioManager.instance.SetListenerData(position, velocity);
    }

    /**
     * Plays a sound wihout any 3D effects from the interface audiosource pool.
     * @param name
     *      - name of the file, including its file extension.
     * @param volume
     *      - the sound's gain
     * @param pitch
     *      - the sound's pitch
     */
    public static void PlayInterfaceSound(String name, float volume, float pitch){
        AudioManager.instance.PlayInterfaceSound(name, volume, pitch);
    }
}
