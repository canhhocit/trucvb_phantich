package com.TrucVanban.registry.validator;

import com.TrucVanban.shared.exception.BusinessLogicException;

import java.util.Map;
import java.util.Set;

public abstract class AbstractStateTransitionValidator<T extends Enum<T>> implements StateTransitionValidator<T> {
    // đn các transition hợp lệ currentState -> Set of valid targetStates
    protected abstract Map<T, Set<T>> getTransitions();
    // return entity name hiển thị trong message lỗi
    protected abstract String getEntityName();
    // các state bb cần lý do
    protected abstract Set<T> getStatesRequiringReason();

    @Override
    public void validate(String entityIdentifier, T currentState, T targetState, String reason) {
        validateTransitionAllowed(entityIdentifier, currentState, targetState);
        validateReasonIfRequired(entityIdentifier, targetState, reason);
    }

    //validate check hople currentState -> targetState
    private void validateTransitionAllowed(String entityIdentifier, T currentState, T targetState) {
        Set<T> allowedTargetStates = getTransitions().get(currentState);

        if (allowedTargetStates == null || !allowedTargetStates.contains(targetState)) {
            throw new BusinessLogicException(
                    String.format("Không thể chuyển trạng thái %s '%s' từ %s sang %s",
                            getEntityName(), entityIdentifier, currentState, targetState));
        }
    }

    // validate reason chuyển state
    private void validateReasonIfRequired(String entityIdentifier, T targetState, String reason) {
        if (getStatesRequiringReason().contains(targetState)) {
            if (reason == null || reason.isBlank()) {
                throw new BusinessLogicException(
                        String.format("Lý do là bắt buộc khi chuyển %s '%s' sang trạng thái %s",
                                getEntityName(), entityIdentifier, targetState));
            }
        }
    }
}
