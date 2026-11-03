import { NextResponse } from "next/server"
import { clientIp, coreFetch, type TokenPair } from "@/_lib/core/client"
import { problemResponse, unavailable } from "@/_lib/core/bff"
import { setSession } from "@/_lib/core/session"

export async function POST(req: Request) {
    const { email, password } = await req.json()

    let res: Response
    try {
        res = await coreFetch("/api/v1/auth/login", {
            method: "POST",
            body: { email, password },
            clientIp: clientIp(req.headers),
        })
    } catch {
        return unavailable()
    }
    if (!res.ok) return problemResponse(res)

    return setSession(NextResponse.json({ success: true }), (await res.json()) as TokenPair)
}
