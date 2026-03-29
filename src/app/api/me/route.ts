import { NextResponse } from "next/server"
import { jwtVerify } from "jose"
import { cookies } from "next/headers"
import { users } from "@/_lib/db"

const SECRET = new TextEncoder().encode(process.env.JWT_SECRET)

export async function GET() {
    try {
        const cookieStore = await cookies()
        console.log("Cookies disponíveis:", cookieStore.getAll())
        const token = cookieStore.get("token")?.value

        if (!token) {
            return NextResponse.json({ user: null }, { status: 401 })
        }

        const { payload } = await jwtVerify(token, SECRET)
        const user = users.find((u) => u.id === payload.id)

        if (!user) {
            return NextResponse.json({ user: null }, { status: 404 })
        }

        const safeUser = {
            id: user.id,
            email: user.email,
            phone: user.phone,
            fullName: user.fullName,
            avatarUrl: user.avatarUrl,
        }

        return NextResponse.json({ user: safeUser })
    } catch (err) {
        console.error("Erro em /api/me:", err)
        return NextResponse.json({ user: null }, { status: 401 })
    }
}
