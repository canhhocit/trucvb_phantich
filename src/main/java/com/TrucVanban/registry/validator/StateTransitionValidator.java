package com.TrucVanban.registry.validator;

public interface StateTransitionValidator<T extends Enum<T>> {

    //entityIdentifier: định anh entity(code, id, ...)
    //current/targetState: trạng thái htai/muctieu
    //reason: lý do chuyển(optional)
    void validate(String entityIdentifier, T currentState, T targetState, String reason);
}
