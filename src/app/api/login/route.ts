import { NextResponse } from "next/server"
import bcrypt from "bcryptjs"
import { SignJWT } from "jose"
import { users } from "@/_lib/db"

const SECRET = new TextEncoder().encode(process.env.JWT_SECRET)

export async function POST(req: Request) {
    const { email, password } = await req.json()

    const user = users.find((u) => u.email === email.toLowerCase())
    if (!user) return NextResponse.json({ error: "Usuário não encontrado" }, { status: 401 })

    const valid = await bcrypt.compare(password, user.password)
    if (!valid) return NextResponse.json({ error: "Senha incorreta" }, { status: 401 })

    const token = await new SignJWT({ id: user.id, email: user.email })
        .setProtectedHeader({ alg: "HS256" })
        .setExpirationTime("1h")
        .sign(SECRET)

    const res = NextResponse.json({ success: true, user: { id: user.id, email: user.email } })
    res.cookies.set("token", token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        path: "/",
        sameSite: "lax",
        maxAge: 60 * 60,
    })

    return res
}