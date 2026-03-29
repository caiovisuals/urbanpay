import { NextResponse } from "next/server"
import { users, Piggy } from "@/_lib/db"

function getUserByToken(token: string | undefined) {
    if (!token) return null
    return users.find((user) => user.token === token) || null
}

export async function GET(req: Request) {
    const cookie = req.headers.get("cookie") || ""
    const token = cookie.split("token=")[1]?.split(";")[0]

    const user = getUserByToken(token)

    if (!user) {
        return NextResponse.json({ error: "Usuário não autenticado" }, { status: 401 })
    }

    return NextResponse.json(user.piggys || [])
}

export async function POST(req: Request) {
    const cookie = req.headers.get("cookie") || ""
    const token = cookie.split("token=")[1]?.split(";")[0]

    const user = getUserByToken(token)

    if (!user) {
        return NextResponse.json({ error: "Usuário não autenticado" }, { status: 401 })
    }

    const body = await req.json()
    const { name, goal } = body

    if (!name) {
        return NextResponse.json({ error: "Nome do confrinho é obrigatório" }, { status: 400 })
    }

    const newPiggy: Piggy = {
        id: Date.now(),
        name,
        value: 0,
        goal: goal || undefined,
        createdAt: new Date(),
    }

    if (!user.piggys) user.piggys = []
    user.piggys.push(newPiggy)

    return NextResponse.json(newPiggy, { status: 201 })
}