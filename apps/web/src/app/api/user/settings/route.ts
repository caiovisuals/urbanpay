import { NextResponse } from "next/server"
import { callCoreAsUser } from "@/_lib/core/bff"
import type { User } from "@/_lib/types"

/**
 * Dados do usuário logado (identificado pelo token, nunca por ?userId=).
 * A versão antiga devolvia o usuário de qualquer id, inclusive o hash da senha.
 */
export async function GET() {
    return callCoreAsUser<User>("/api/v1/me")
}

export async function PATCH() {
    return NextResponse.json(
        { error: "Edição de perfil ainda não disponível (será migrada para o core)" },
        { status: 501 },
    )
}