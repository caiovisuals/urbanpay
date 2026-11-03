/**
 * Cliente HTTP do BFF para o serviço Java (services/core).
 * Só roda no servidor (route handlers / proxy). O navegador nunca fala direto com o core
 * e nunca vê os tokens: eles ficam em cookies httpOnly.
 */

export const CORE_API_URL = process.env.CORE_API_URL ?? "http://localhost:8080"

export type TokenPair = {
    accessToken: string
    accessTokenExpiresAt: string
    refreshToken: string
    refreshTokenExpiresAt: string
}

/** Erro no formato RFC 9457 (Problem Details) devolvido pelo core. */
export type Problem = {
    status?: number
    code?: string
    detail?: string
    fields?: Record<string, string>
}

/**
 * IP real do usuário, para o rate limit do core.
 * O X-Forwarded-For é uma lista "cliente, proxy1, proxy2"; só as entradas adicionadas pelos
 * NOSSOS proxies são confiáveis (o resto o próprio cliente pode forjar). Por isso lemos da
 * direita para a esquerda, pulando TRUSTED_PROXY_HOPS - 1 entradas.
 * Em produção, o Next deve ficar atrás de um load balancer que acrescenta o IP de quem conectou.
 */
export function clientIp(headers: Headers): string | undefined {
    const hops = Math.max(1, Number(process.env.TRUSTED_PROXY_HOPS ?? 1))
    const chain = (headers.get("x-forwarded-for") ?? "")
        .split(",")
        .map((ip) => ip.trim())
        .filter(Boolean)
    return chain[chain.length - hops] ?? chain[0]
}

type CoreRequest = {
    method?: "GET" | "POST" | "PATCH" | "DELETE"
    body?: unknown
    accessToken?: string
    headers?: Record<string, string>
    clientIp?: string
}

export async function coreFetch(path: string, req: CoreRequest = {}): Promise<Response> {
    const headers: Record<string, string> = { Accept: "application/json", ...req.headers }
    if (req.body !== undefined) headers["Content-Type"] = "application/json"
    if (req.accessToken) headers.Authorization = `Bearer ${req.accessToken}`
    if (req.clientIp) headers["X-Forwarded-For"] = req.clientIp

    return fetch(`${CORE_API_URL}${path}`, {
        method: req.method ?? "GET",
        headers,
        body: req.body === undefined ? undefined : JSON.stringify(req.body),
        cache: "no-store",
        signal: AbortSignal.timeout(10_000),
    })
}

export async function readProblem(res: Response): Promise<Problem> {
    try {
        return (await res.json()) as Problem
    } catch {
        return { status: res.status }
    }
}

export async function refreshTokens(refreshToken: string | undefined): Promise<TokenPair | null> {
    if (!refreshToken) return null
    try {
        const res = await coreFetch("/api/v1/auth/refresh", { method: "POST", body: { refreshToken } })
        return res.ok ? ((await res.json()) as TokenPair) : null
    } catch {
        return null
    }
}
