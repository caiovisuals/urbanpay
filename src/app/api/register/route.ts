import { NextResponse } from "next/server"
import bcrypt from "bcryptjs"
import { SignJWT } from "jose"
import { users, User } from "@/_lib/db"

const SECRET = new TextEncoder().encode(process.env.JWT_SECRET)

function isValidPassword(password: string): boolean {
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/
    return regex.test(password)
}

function isAdult(birthDate: Date): boolean {
    const today = new Date()
    const age = today.getFullYear() - birthDate.getFullYear()
    const hasBirthdayPassed =
        today.getMonth() > birthDate.getMonth() ||
        (today.getMonth() === birthDate.getMonth() && today.getDate() >= birthDate.getDate())
    return age > 18 || (age === 18 && hasBirthdayPassed)
}

function isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

function isValidCPF(cpf: string): boolean {
    cpf = cpf.replace(/\D/g, "")
    if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) return false

    let sum = 0
    let remainder

    for (let i = 1; i <= 9; i++) {
      sum += parseInt(cpf.substring(i - 1, i)) * (11 - i)
    }
    remainder = (sum * 10) % 11
    if (remainder === 10 || remainder === 11) remainder = 0
    if (remainder !== parseInt(cpf.substring(9, 10))) return false

    sum = 0
    for (let i = 1; i <= 10; i++) {
      sum += parseInt(cpf.substring(i - 1, i)) * (12 - i)
    }
    remainder = (sum * 10) % 11
    if (remainder === 10 || remainder === 11) remainder = 0
    return remainder === parseInt(cpf.substring(10, 11))
}

export async function POST(req: Request) {
    const { email, password, phone, cpf, pin, rg, fullName, birthDate } = await req.json()

    if (!email || !password || !phone || !cpf || !pin) {
        return NextResponse.json({ error: "Preencha todos os campos obrigatórios" }, { status: 400 })
    }

    if (!isValidPassword(password)) {
        return NextResponse.json(
            { error: "A senha deve ter no mínimo 8 caracteres, incluindo maiúsculas, minúsculas, número e símbolo" },
            { status: 400 }
        )
    }

    if (!isAdult(new Date(birthDate))) {
        return NextResponse.json({ error: "É necessário ter pelo menos 18 anos para criar uma conta" }, { status: 400 })
    }

    if (!isValidEmail(email)) {
        return NextResponse.json({ error: "Email inválido" }, { status: 400 })
    }

    if (!isValidCPF(cpf)) {
        return NextResponse.json({ error: "CPF inválido" }, { status: 400 })
    }

    if (users.find((u) => u.email === email)) {
        return NextResponse.json({ error: "Usuário já existe" }, { status: 400 })
    }

    const hashed = await bcrypt.hash(password, 10)

    const newUser: User = {
        id: users.length + 1,
        email: email.toLowerCase(),
        password: hashed,
        phone,
        cpf,
        pin,
        rg,
        fullName: fullName || "",
        birthDate: birthDate ? new Date(birthDate) : new Date(),
        balance: 0,
        createdAt: new Date(),
    }

    users.push(newUser)

    const token = await new SignJWT({ id: newUser.id, email: newUser.email })
        .setProtectedHeader({ alg: "HS256" })
        .setExpirationTime("1h")
        .sign(SECRET)

    const res = NextResponse.json({ success: true, user: { id: newUser.id, email: newUser.email } })
    res.cookies.set("token", token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === "production",
        path: "/",
        sameSite: "lax",
        maxAge: 60 * 60,
    })

    return res
}