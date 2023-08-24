package james.UI;

import james.Audio.*;

public class ButtonAudio implements ButtonListener {

    private String hoverSound, clickSound, pressSound, hoverExitSound, toggleOnSound, toggleOffSound;
    private float hoverVolume, clickVolume, pressVolume, hoverExitVolume, toggleOnVolume, toggleOffVolume;
    private float hoverPitch, clickPitch, pressPitch, hoverExitPitch, toggleOnPitch, toggleOffPitch;
    private float volume, pitch;

    /**
     * Constructor for a standard button audio callback class. Leave parameters blank that are to have no audio callback. Otherwise, parameters are to be the names of their
     * respective files, including file extension. Volume and pitch are any floats with common values of 1.0.
     */
    public ButtonAudio(String hoverSound, String clickSound, String pressSound, String hoverExitSound, String toggleOnSound, String toggleOffSound, float volume, float pitch){
        this.hoverSound = hoverSound;
        this.clickSound = clickSound; 
        this.pressSound = pressSound;
        this.hoverExitSound = hoverExitSound;
        this.toggleOnSound = toggleOnSound;
        this.toggleOffSound = toggleOffSound;
        this.volume = volume;
        this.pitch = pitch;    
        hoverVolume = clickVolume = pressVolume = hoverExitVolume = toggleOnVolume = toggleOffVolume = 1.0f;
        hoverPitch = clickPitch = pressPitch = hoverExitPitch = toggleOnPitch = toggleOffPitch = 1.0f;
    }

    /**
     * Sets the volume multipliers for each callback audioclip.
     */
    public void SetCallbackVolumes(float hoverVolume, float clickVolume, float pressVolume, float hoverExitVolume, float toggleOnVolume, float toggleOffVolume){
        this.hoverVolume = hoverVolume;
        this.clickVolume = clickVolume;
        this.pressVolume = pressVolume;
        this.hoverExitVolume = hoverExitVolume;
        this.toggleOnVolume = toggleOnVolume;
        this.toggleOffVolume = toggleOffVolume;
    }

    
    /**
     * Sets the pitch multipliers for each callback audioclip.
     */
    public void SetCallbackPitches(float hoverPitch, float clickPitch, float pressPitch, float hoverExitPitch, float toggleOnPitch, float toggleOffPitch){
        this.hoverPitch = hoverPitch;
        this.clickPitch = clickPitch;
        this.pressPitch = pressPitch;
        this.hoverExitPitch = hoverExitPitch;
        this.toggleOnPitch = toggleOnPitch;
        this.toggleOffPitch = toggleOffPitch;
    }

    public void OnHover() {
        if(!hoverSound.isEmpty()) AudioLibrary.PlayInterfaceSound(hoverSound, hoverVolume * volume, hoverPitch * pitch);
    }

    public void OnClick() {
        if(!clickSound.isEmpty()) AudioLibrary.PlayInterfaceSound(clickSound, clickVolume * volume, clickPitch * pitch);
    }

    public void OnPress() {
        if(!pressSound.isEmpty()) AudioLibrary.PlayInterfaceSound(pressSound, pressVolume * volume, pressPitch * pitch);
    }

    public void OnHoverExit() {
        if(!hoverExitSound.isEmpty()) AudioLibrary.PlayInterfaceSound(hoverExitSound, hoverExitVolume * volume, hoverExitPitch * pitch);
    }

    public void OnToggle() {
    }

    public void OnToggleOn() {
        if(!toggleOnSound.isEmpty()) AudioLibrary.PlayInterfaceSound(toggleOnSound, toggleOnVolume * volume, toggleOnPitch * pitch);
    }

    public void OnToggleOff() {
        if(!toggleOffSound.isEmpty()) AudioLibrary.PlayInterfaceSound(toggleOffSound, toggleOffVolume * volume, toggleOffPitch * pitch);
    }
    
}
