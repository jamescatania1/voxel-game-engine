package james.UI;

/**
 * Abstract Constraint class for Transform GUI components.
 * One of { AbsoluteConstraint, RelativeConstraint, ScaledRelativeConstraint, AspectConstraint }.
 */
public abstract class Constraint {
    protected float value;
    abstract void update(Transform transform);
}