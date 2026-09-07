"use client";

import { jwtDecode } from "jwt-decode";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { clearToken, getToken, setToken } from "@/lib/api";
import type { JwtPayload, Role } from "@/lib/types";

interface AuthUser {
  email: string;
  role: Role;
  profileId: number;
}

interface AuthContextValue {
  user: AuthUser | null;
  /** 초기 토큰 복원이 끝났는지 여부 (하이드레이션 가드) */
  initialized: boolean;
  login: (accessToken: string) => AuthUser;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function decode(token: string): AuthUser | null {
  try {
    const payload = jwtDecode<JwtPayload>(token);
    if (!payload.exp || payload.exp * 1000 <= Date.now()) {
      return null; // 만료
    }
    return {
      email: payload.sub,
      role: payload.role,
      profileId: payload.profileId,
    };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    const token = getToken();
    if (token) {
      const decoded = decode(token);
      if (decoded) {
        setUser(decoded);
      } else {
        clearToken();
      }
    }
    setInitialized(true);
  }, []);

  const login = useCallback((accessToken: string): AuthUser => {
    const decoded = decode(accessToken);
    if (!decoded) {
      throw new Error("유효하지 않은 토큰입니다.");
    }
    setToken(accessToken);
    setUser(decoded);
    return decoded;
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, initialized, login, logout }),
    [user, initialized, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
