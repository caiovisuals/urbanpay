import { cookies } from "next/headers"
import { NextResponse } from "next/server"
import { coreFetch } from "@/_lib/core/client"
import { REFRESH_COOKIE, clearSession } from "@/_lib/core/session"

export async function POST() {
    const refreshToken = (await cookies()).get(REFRESH_COOKIE)?.value
    if (refreshToken) {
        await coreFetch("/api/v1/auth/logout", { method: "POST", body: { refreshToken } }).catch(() => undefined)
    }
    return clearSession(NextResponse.json({ success: true }))
}