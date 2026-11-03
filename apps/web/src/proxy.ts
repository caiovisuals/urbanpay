import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"
import { createRemoteJWKSet, jwtVerify } from "jose"
import { CORE_API_URL, refreshTokens } from "@/_lib/core/client"
import { ACCESS_COOKIE, REFRESH_COOKIE, clearSession, setSession } from "@/_lib/core/session"

/**
 * Proteção das páginas (antes ficava em /middleware.ts, que o Next ignorava por causa da pasta src/).
 * O token é validado com a chave PÚBLICA do core (JWKS, RS256): o front não guarda segredo.
 */
const JWKS = createRemoteJWKSet(new URL(`${CORE_API_URL}/.well-known/jwks.json`))
const ISSUER = process.env.JWT_ISSUER ?? "urbanpay-core"

const PRIVATE = ["/dashboard", "/transfer", "/cards", "/piggys", "/loans", "/investments", "/settings", "/assistance"]
const AUTH_PAGES = ["/login", "/register", "/forgot-password"]

const matches = (path: string, list: string[]) => list.some((p) => path === p || path.startsWith(`${p}/`))

async function isValid(token: string | undefined): Promise<boolean> {
    if (!token) return false
    try {
        await jwtVerify(token, JWKS, { issuer: ISSUER, audience: "urbanpay", algorithms: ["RS256"] })
        return true
    } catch {
        return false
    }
}

export async function proxy(req: NextRequest) {
    const path = req.nextUrl.pathname
    const isPrivate = matches(path, PRIVATE)
    const isAuthPage = matches(path, AUTH_PAGES)
    if (!isPrivate && !isAuthPage) return NextResponse.next()

    let authenticated = await isValid(req.cookies.get(ACCESS_COOKIE)?.value)
    const rotated = authenticated ? null : await refreshTokens(req.cookies.get(REFRESH_COOKIE)?.value)
    authenticated ||= rotated !== null

    let res: NextResponse
    if (isPrivate && !authenticated) {
        const url = new URL("/login", req.url)
        url.searchParams.set("next", path)
        return clearSession(NextResponse.redirect(url))
    } else if (isAuthPage && authenticated) {
        res = NextResponse.redirect(new URL("/dashboard", req.url))
    } else {
        res = NextResponse.next()
    }
    return rotated ? setSession(res, rotated) : res
}

export const config = {
    matcher: ["/((?!api|_next/static|_next/image|assets|favicon.ico|default-avatar.png).*)"],
}
