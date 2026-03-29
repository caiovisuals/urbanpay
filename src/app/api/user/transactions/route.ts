import { NextResponse } from "next/server"
import { users, Transaction } from "@/_lib/db"

function getUserByToken(token?: string) {
    if (!token) return null
    return users.find(user => user.token === token)
}

export async function GET(req: Request) {
    const cookie = req.headers.get("cookie") || ""
    const token = cookie.split("token=")[1]?.split(";")[0]

    const user = getUserByToken(token)
    if (!user) return NextResponse.json({ error: "Usuário não autenticado" }, { status: 401 })

    return NextResponse.json({ transactions: user.transactions || [] })
}

export async function POST(req: Request) {
    const cookie = req.headers.get("cookie") || ""
    const token = cookie.split("token=")[1]?.split(";")[0]

    const user = getUserByToken(token)
    if (!user) return NextResponse.json({ error: "Usuário não autenticado" }, { status: 401 })

    const { value, type, description }: { value: number; type: "inflow" | "out"; description?: string } = await req.json()

    if (!user.transactions) user.transactions = []

    const transaction: Transaction = {
        id: user.transactions.length + 1,
        status: "completed",
        value,
        date: new Date(),
        description,
        method: "internal",
        fromUserId: type === "out" ? user.id : undefined,
        toUserId: type === "inflow" ? user.id : undefined,
    }

    user.transactions.push(transaction)

    return NextResponse.json({ transaction })
}