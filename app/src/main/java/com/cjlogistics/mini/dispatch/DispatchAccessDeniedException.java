package com.cjlogistics.mini.dispatch;
public class DispatchAccessDeniedException extends RuntimeException {
    public DispatchAccessDeniedException(Long dispatchId) { super("배차를 조작할 권한이 없습니다: " + dispatchId); }
}
