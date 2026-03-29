import { NextResponse } from "next/server"
import type { NextRequest } from "next/server"
import { users, User } from "@/_lib/db"

export async function GET(req: NextRequest) {
    const userId = Number(req.nextUrl.searchParams.get("userId"))
    if (!userId) return NextResponse.json({ error: "User ID is required" }, { status: 400 })

    const user = users.find(u => u.id === userId)
    if (!user) return NextResponse.json({ error: "User not found" }, { status: 404 })

    return NextResponse.json(user)
}

export async function PATCH(req: NextRequest) {
    try {
        const { userId, fullName, email, phone, avatarUrl } = await req.json()

        if (!userId) {
            return NextResponse.json({ error: "User ID is required" }, { status: 400 })
        }

        const userIndex = users.findIndex(u => u.id === userId)
        if (userIndex === -1) {
            return NextResponse.json({ error: "User not found" }, { status: 404 })
        }

        if (fullName !== undefined) users[userIndex].fullName = fullName
        if (email !== undefined) users[userIndex].email = email
        if (phone !== undefined) users[userIndex].phone = phone
        if (avatarUrl !== undefined) users[userIndex].avatarUrl = avatarUrl

        return NextResponse.json(users[userIndex])
    } catch (error) {
        console.error(error)
        return NextResponse.json({ error: "Invalid request" }, { status: 400 })
    }
}