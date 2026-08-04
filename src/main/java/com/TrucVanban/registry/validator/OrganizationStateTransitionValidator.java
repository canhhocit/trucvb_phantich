package com.TrucVanban.registry.validator;

import com.TrucVanban.registry.enums.OrganizationStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/*
 PENDING_APPROVAL -> ACTIVE (approve)
 PENDING_APPROVAL -> REJECTED (reject, reason)
 ACTIVE -> SUSPENDED (suspend, reason)
 SUSPENDED -> ACTIVE (reactivate)
 REJECTED -> k chuyen dc
 */
@Component
public class OrganizationStateTransitionValidator extends AbstractStateTransitionValidator<OrganizationStatus> {

    private static final Map<OrganizationStatus, Set<OrganizationStatus>> TRANSITIONS = Map.of(
            OrganizationStatus.PENDING_APPROVAL, Set.of(OrganizationStatus.ACTIVE, OrganizationStatus.REJECTED),
            OrganizationStatus.ACTIVE, Set.of(OrganizationStatus.SUSPENDED),
            OrganizationStatus.SUSPENDED, Set.of(OrganizationStatus.ACTIVE),
            OrganizationStatus.REJECTED, Set.of()
    );

    private static final Set<OrganizationStatus> STATES_REQUIRING_REASON = Set.of(
            OrganizationStatus.REJECTED,
            OrganizationStatus.SUSPENDED
    );

    @Override
    protected Map<OrganizationStatus, Set<OrganizationStatus>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected String getEntityName() {
        return "tổ chức";
    }

    @Override
    protected Set<OrganizationStatus> getStatesRequiringReason() {
        return STATES_REQUIRING_REASON;
    }
}
