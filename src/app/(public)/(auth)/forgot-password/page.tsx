"use client"

import { useState } from "react"
import Link from "next/link"

export default function ForgotPasswordPage() {
    const [email, setEmail] = useState("")
    const [message, setMessage] = useState("")
    const [loading, setLoading] = useState(false)

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        setLoading(true)
        setMessage("")

        try {
            const res = await fetch("/api/forgot-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email }),
            })

            const data = await res.json()

            if (!res.ok) {
                setMessage(data.error || "Erro ao solicitar redefinição.")
            } else {
                setMessage("Se este e-mail estiver cadastrado, enviaremos um link de recuperação.")
            }
        } catch {
            setMessage("Erro de rede. Tente novamente.")
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="flex justify-center items-center h-full">
            <form onSubmit={handleSubmit} className="bg-gray-800 p-8 rounded-2xl w-full max-w-md space-y-4 shadow-lg">
                <h1 className="text-2xl font-bold text-blue-400 mb-4">Recuperar Senha</h1>
                {message && <p className="text-sm text-center">{message}</p>}
                <input
                    type="email"
                    placeholder="Digite seu email"
                    className="w-full p-3 rounded bg-gray-700"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                />
                <button
                    type="submit"
                    disabled={loading}
                    className="w-full bg-blue-600 py-3 rounded hover:bg-blue-500"
                >
                    {loading ? "Enviando..." : "Enviar link de recuperação"}
                </button>
            </form>
        </div>
    )
}
