import { NextResponse } from "next/server"
import { users } from "@/_lib/db"

function getUserByToken(token?: string) {
    if (!token) return null
    return users.find(user => user.token === token)
}

export async function GET(req: Request) {
    const cookie = req.headers.get("cookie") || ""
    const token = cookie.split("token=")[1]?.split(";")[0]

    const user = getUserByToken(token)
    if (!user) return NextResponse.json({ error: "Usuário não autenticado" }, { status: 401 })

    return NextResponse.json({ balance: user.balance })
}

export async function POST(req: Request) {
    const cookie = req.headers.get("cookie") || ""
    const token = cookie.split("token=")[1]?.split(";")[0]

    const user = getUserByToken(token)
    if (!user) return NextResponse.json({ error: "Usuário não autenticado" }, { status: 401 })

    const { value, type }: { value: number; type: "inflow" | "out" } = await req.json()

    if (typeof user.balance !== "number") user.balance = 0

    if (type === "inflow") user.balance += value
    else if (type === "out") user.balance -= value

    return NextResponse.json({ balance: user.balance })
}