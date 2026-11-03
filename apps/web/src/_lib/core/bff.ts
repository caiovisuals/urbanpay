import { cookies } from "next/headers"
import { NextResponse } from "next/server"
import { coreFetch, readProblem, refreshTokens, type TokenPair } from "./client"
import { ACCESS_COOKIE, REFRESH_COOKIE, clearSession, setSession } from "./session"

type CoreCall = Parameters<typeof coreFetch>[1]

/** Converte o Problem Details do core no formato que as telas já usam: { error, code, fields }. */
export async function problemResponse(res: Response): Promise<NextResponse> {
    const problem = await readProblem(res)
    return NextResponse.json(
        { error: problem.detail ?? "Erro inesperado", code: problem.code, fields: problem.fields },
        { status: res.status },
    )
}

export function unavailable(): NextResponse {
    return NextResponse.json({ error: "Serviço indisponível. Tente novamente." }, { status: 503 })
}

/**
 * Chama o core em nome do usuário logado. Se o access token expirou, usa o refresh token
 * (com rotação) e repete a chamada uma vez — seguro inclusive para POST de transferência,
 * porque a operação é idempotente pela Idempotency-Key.
 */
export async function callCoreAsUser<T>(
    path: string,
    init: Omit<CoreCall, "accessToken"> = {},
    mapBody: (body: T) => unknown = (b) => b,
    successStatus?: number,
): Promise<NextResponse> {
    const store = await cookies()
    let accessToken = store.get(ACCESS_COOKIE)?.value
    let rotated: TokenPair | null = null

    if (!accessToken) {
        rotated = await refreshTokens(store.get(REFRESH_COOKIE)?.value)
        accessToken = rotated?.accessToken
    }
    if (!accessToken) {
        return clearSession(NextResponse.json({ error: "Usuário não autenticado" }, { status: 401 }))
    }

    let res: Response
    try {
        res = await coreFetch(path, { ...init, accessToken })
        if (res.status === 401 && !rotated) {
            rotated = await refreshTokens(store.get(REFRESH_COOKIE)?.value)
            if (rotated) res = await coreFetch(path, { ...init, accessToken: rotated.accessToken })
        }
    } catch {
        return unavailable()
    }

    let out: NextResponse
    if (res.ok) {
        const body = res.status === 204 ? null : ((await res.json()) as T)
        out = NextResponse.json(body === null ? null : mapBody(body), { status: successStatus ?? res.status })
    } else {
        out = await problemResponse(res)
    }

    if (rotated) setSession(out, rotated)
    else if (res.status === 401) clearSession(out)
    return out
}
