package smartfloor.repository.jpa.projections;

public interface UserProjection {
    Long getId();

    String getAuthId();

    String firstName();

    String lastName();
}
