"use client"

import Link from "next/link"
import Image from "next/image"
import { useEffect, useState } from "react"
import type { User } from "@/_lib/db"

export default function HomeHeader() {
    const [user, setUser] = useState<User | null>(null)

    useEffect(() => {
        async function fetchUser() {
            try {
                const res = await fetch("/api/me", { credentials: "include" })
                if (res.ok) {
                    const data = await res.json()
                    setUser(data.user || null)
                }
            } catch (err) {
                console.error("Erro ao buscar usuário:", err)
            }
        }
        fetchUser()
    }, [])
    
    return (
        <header className="w-full h-16 px-6 bg-gray-800 text-white flex items-center justify-center">
            <Link href="/dashboard" className="w-[20%] flex justify-start items-center">
                <h1 className="text-xl font-bold">UrbanPay</h1>
            </Link>
            <div className="w-[60%] flex justify-center items-center">
            </div>
            <Link href="/settings" className="w-[20%] flex flex-row gap-2 justify-end items-center">
                <div className="size-10 relative border-2 border-white rounded-full overflow-hidden">
                    <Image src={user?.avatarUrl || "/default-avatar.png"} alt="User Avatar" className="select-none rounded-full object-cover" draggable="false" fill />
                </div>
                <div className="flex flex-col items-start justify-center">
                    <span className="leading-tight">{user?.fullName || "Nome do usuário"}</span>
                    <p className="text-[14px] leading-tight">Ver seu perfil</p>
                </div>
            </Link>
        </header>
    )
}