package factory.model;

@FunctionalInterface
public interface PartFactory<T extends CarPart> {
    T create();
}
