package com.cjlogistics.mini.dispatch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DispatchStateMachineTest {

    private Dispatch newDispatch() {
        return new Dispatch(100L, 50L, 130.0);
    }

    @Test
    void initial_status_is_PROPOSED() {
        Dispatch d = newDispatch();
        assertThat(d.getStatus()).isEqualTo(DispatchStatus.PROPOSED);
    }

    @Test
    void accept_from_PROPOSED_succeeds() {
        Dispatch d = newDispatch();
        d.accept();
        assertThat(d.getStatus()).isEqualTo(DispatchStatus.ACCEPTED);
    }

    @Test
    void reject_from_PROPOSED_succeeds() {
        Dispatch d = newDispatch();
        d.reject();
        assertThat(d.getStatus()).isEqualTo(DispatchStatus.REJECTED);
    }

    @Test
    void markCompleted_from_ACCEPTED_succeeds() {
        Dispatch d = newDispatch();
        d.accept();
        d.markCompleted();
        assertThat(d.getStatus()).isEqualTo(DispatchStatus.COMPLETED);
    }

    @Test
    void accept_twice_throws() {
        Dispatch d = newDispatch();
        d.accept();
        assertThatThrownBy(d::accept)
                .isInstanceOf(InvalidDispatchStatusTransitionException.class);
    }

    @Test
    void reject_after_accept_throws() {
        Dispatch d = newDispatch();
        d.accept();
        assertThatThrownBy(d::reject)
                .isInstanceOf(InvalidDispatchStatusTransitionException.class);
    }

    @Test
    void markCompleted_before_accept_throws() {
        Dispatch d = newDispatch();
        assertThatThrownBy(d::markCompleted)
                .isInstanceOf(InvalidDispatchStatusTransitionException.class);
    }
}
