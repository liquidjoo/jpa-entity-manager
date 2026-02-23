package persistence.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class EntityEntry {
    private final Object entity;
    private Object[] loadedState;  // 스냅샷
    private EntityStatus status;

    public enum EntityStatus {
        MANAGED,    // 영속 상태
        DELETED     // 삭제 예약
    }

    public EntityEntry(Object entity, Object[] loadedState) {
        this.entity = entity;
        // 불변 복사: 원본 배열이 변경되어도 스냅샷은 변경되지 않음
        this.loadedState = Arrays.copyOf(loadedState, loadedState.length);
        this.status = EntityStatus.MANAGED;
    }

    public boolean isDirty(Object[] currentState) {
        return !Arrays.equals(loadedState, currentState);
    }


    // 변경된 필드의 인덱스 반환
    public int[] findModified(Object[] currentState) {
        List<Integer> modified = new ArrayList<>();
        for (int i = 0; i < loadedState.length; i++) {
            if (!Objects.equals(loadedState[i], currentState[i])) {
                modified.add(i);
            }
        }
        return modified.stream().mapToInt(i -> i).toArray();
    }

    public void updateSnapshot(Object[] newState) {
        this.loadedState = Arrays.copyOf(newState, newState.length);
    }

    public Object getEntity() {
        return entity;
    }

    public Object[] getLoadedState() {
        return loadedState;
    }

    public EntityStatus getStatus() {
        return status;
    }

    public void setStatus(EntityStatus status) {
        this.status = status;
    }
}
