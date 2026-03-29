import { NextResponse } from "next/server"
import { users } from "@/_lib/db"
import { jwtVerify } from "jose"

const SECRET = new TextEncoder().encode(process.env.JWT_SECRET)

export async function DELETE(req: Request) {
    const cookie = req.headers.get("cookie") || ""
    const token = cookie.split("token=")[1]?.split(";")[0]

    if (!token) {
        return NextResponse.json({ error: "Não autorizado" }, { status: 401 })
    }

    try {
        const { payload } = await jwtVerify(token, SECRET)
        const id = payload.id as number

        const index = users.findIndex((u) => u.id === id)
        if (index === -1) {
        return NextResponse.json({ error: "Usuário não encontrado" }, { status: 404 })
        }

        users.splice(index, 1)

        const res = NextResponse.json({ success: true })
        res.cookies.set("token", "", { path: "/", maxAge: 0 })
        return res
    } catch {
        return NextResponse.json({ error: "Token inválido" }, { status: 401 })
    }
}
