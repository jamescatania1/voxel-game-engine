package james.UI;

/**
 * Interface for GUI button callback functions.
 */
public interface ButtonListener {
    
    /**
     * Called when the button is entered while in an interactable state.
     */
    void OnHover();

    /**
     * Called whenever the button is clicked whilst able to be clicked.
     */
    void OnClick();

    /**
     * Called whenever the button is pressed whilst able to be pressed.
     */   
    void OnPress();

    /**
     * Called when the hovered button is exited.
     */
    void OnHoverExit();
    
    /**
     * Called whenever a toggle change occurs (code or button interaction).
     */
    void OnToggle();

    /**
     * Called whenever the button is toggled on (code or button interaction).
     */
    void OnToggleOn();
    
    /**
     * Called whenever the button is toggled off (code or button interaction).
     */
    void OnToggleOff();
}