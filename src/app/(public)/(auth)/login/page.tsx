"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"

export default function LoginPage() {
    const router = useRouter()
    const [form, setForm] = useState({ email: "", password: "" })
    const [error, setError] = useState("")
    const [loading, setLoading] = useState(false)

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        setError("")
        setLoading(true)

        try {
            const res = await fetch("/api/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(form),
                credentials: "include",
            })

            const data = await res.json()

            if (!res.ok) {
                setError(data.error || "Erro ao logar")
            } else {
                router.push("/dashboard")
            }
        } catch {
            setError("Erro de rede")
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="flex justify-center items-center h-full">
            <form onSubmit={handleSubmit} className="bg-gray-800 p-8 rounded-2xl w-full max-w-lg space-y-4 shadow-lg">
                <h1 className="text-2xl font-bold text-blue-400 mb-4">Login</h1>
                {error && <p className="text-red-500">{error}</p>}
                <input
                type="email"
                placeholder="Email"
                className="w-full p-3 rounded bg-gray-700"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                />
                <input
                type="password"
                placeholder="Senha"
                className="w-full p-3 rounded bg-gray-700"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                />
                <button type="submit" disabled={loading} className="w-full bg-blue-600 py-3 rounded hover:bg-blue-500">
                {loading ? "Entrando..." : "Entrar"}
                </button>
                <div className="flex flex-row gap-4">
                    <button
                    type="button"
                    onClick={() => router.push("/register")}
                    className="py-1.5 px-3 rounded bg-gray-600 hover:bg-gray-500"
                    >
                    Ainda não tem conta?
                    </button>
                    <button
                    type="button"
                    onClick={() => router.push("/forgot-password")}
                    className="py-1.5 px-3 rounded bg-gray-600 hover:bg-gray-500"
                    >
                    Esqueceu a senha?
                    </button>   
                </div>
            </form>
        </div>
    )
}