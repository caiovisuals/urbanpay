import { NextResponse } from "next/server"
import { clientIp, coreFetch, type TokenPair } from "@/_lib/core/client"
import { problemResponse, unavailable } from "@/_lib/core/bff"
import { setSession } from "@/_lib/core/session"

/** Toda a validação (CPF, idade, senha forte, PIN) acontece no core; aqui só repassamos. */
export async function POST(req: Request) {
    const { fullName, email, phone, cpf, birthDate, password, pin } = await req.json()

    let res: Response
    try {
        res = await coreFetch("/api/v1/auth/register", {
            method: "POST",
            clientIp: clientIp(req.headers),
            // O core espera LocalDate ("AAAA-MM-DD"); o formulário envia um Date serializado em ISO.
            body: { fullName, email, phone, cpf, birthDate: String(birthDate ?? "").slice(0, 10), password, pin },
        })
    } catch {
        return unavailable()
    }
    if (!res.ok) return problemResponse(res)

    return setSession(NextResponse.json({ success: true }, { status: 201 }), (await res.json()) as TokenPair)
}
