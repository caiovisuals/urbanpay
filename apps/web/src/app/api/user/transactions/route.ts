import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"
import { callCoreAsUser } from "@/_lib/core/bff"
import type { StatementItem } from "@/_lib/types"

type Statement = { items: StatementItem[]; nextCursor: number | null }

/** Extrato paginado por cursor: /api/user/transactions?cursor=123&limit=20 */
export async function GET(req: NextRequest) {
    const params = new URLSearchParams()
    const cursor = req.nextUrl.searchParams.get("cursor")
    const limit = req.nextUrl.searchParams.get("limit")
    if (cursor && /^\d+$/.test(cursor)) params.set("cursor", cursor)
    if (limit && /^\d+$/.test(limit)) params.set("limit", limit)

    return callCoreAsUser<Statement>(`/api/v1/accounts/me/statement?${params}`, {}, (s) => ({
        transactions: s.items,
        nextCursor: s.nextCursor,
    }))
}

/**
 * Transferência. O cliente DEVE gerar uma Idempotency-Key (ex.: crypto.randomUUID())
 * uma vez por intenção de transferir e reenviá-la em caso de retry.
 */
export async function POST(req: NextRequest) {
    const idempotencyKey = req.headers.get("idempotency-key")
    if (!idempotencyKey) {
        return NextResponse.json({ error: "Cabeçalho Idempotency-Key é obrigatório" }, { status: 400 })
    }
    const { toAccountId, amount, description, pin } = await req.json()

    return callCoreAsUser("/api/v1/transfers", {
        method: "POST",
        headers: { "Idempotency-Key": idempotencyKey },
        body: { toAccountId, amount, description, pin },
    })
}
