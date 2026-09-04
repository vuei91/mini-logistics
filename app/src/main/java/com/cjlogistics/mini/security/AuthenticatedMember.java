package com.cjlogistics.mini.security;
public record AuthenticatedMember(String role, Long profileId, String email) {}
