import type { NextResponse } from "next/server"
import type { TokenPair } from "./client"

export const ACCESS_COOKIE = "up_access"
export const REFRESH_COOKIE = "up_refresh"

const base = {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax" as const,
    path: "/",
}

function secondsUntil(iso: string): number {
    return Math.max(0, Math.floor((new Date(iso).getTime() - Date.now()) / 1000))
}

export function setSession(res: NextResponse, tokens: TokenPair): NextResponse {
    res.cookies.set(ACCESS_COOKIE, tokens.accessToken, { ...base, maxAge: secondsUntil(tokens.accessTokenExpiresAt) })
    res.cookies.set(REFRESH_COOKIE, tokens.refreshToken, { ...base, maxAge: secondsUntil(tokens.refreshTokenExpiresAt) })
    return res
}

export function clearSession(res: NextResponse): NextResponse {
    res.cookies.set(ACCESS_COOKIE, "", { ...base, maxAge: 0 })
    res.cookies.set(REFRESH_COOKIE, "", { ...base, maxAge: 0 })
    // Cookie legado da versão antiga (JWT HS256 local).
    res.cookies.set("token", "", { ...base, maxAge: 0 })
    return res
}
