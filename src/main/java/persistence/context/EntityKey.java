package persistence.context;


public record EntityKey(Class<?> entityType, Object id) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntityKey that)) return false;
        return entityType.equals(that.entityType) && id.equals(that.id);
    }
}
