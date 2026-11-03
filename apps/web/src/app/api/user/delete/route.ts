import { NextResponse } from "next/server"

/** Produto ainda não migrado para o serviço Java (ver roadmap no README). */
function notImplemented() {
    return NextResponse.json({ error: "Funcionalidade em migração para o core" }, { status: 501 })
}

export const DELETE = notImplemented
