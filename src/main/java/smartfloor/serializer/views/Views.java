package smartfloor.serializer.views;

/**
 * This class defines the various role-based views that can be used to serialize entities.
 * For example the {@link Views#User} view defines the fields that should be serialized for a user.
 * In other words, the view defines the fields that a regular user is allowed to see.
 * Following this line of reasoning, a manager is allowed to see additional fields on top of what a regular user can
 * see. Therefore, the {@link Views#Manager} view extends from the {@link Views#User} view, etc.
 * Fields in entity classes to which views apply are annotated with {@link com.fasterxml.jackson.annotation.JsonView}
 * that applies/defines the appropriate view.
 */
public class Views {
    public interface User {
    }

    public interface Manager extends User {
    }

    public interface Admin extends Manager {
    }
}
