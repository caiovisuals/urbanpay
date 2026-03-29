"use client"

import { useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"

function formatCPF(value: string) {
    value = value.replace(/\D/g, "")
    if (value.length <= 11) {
        return value
        .replace(/(\d{3})(\d)/, "$1.$2")
        .replace(/(\d{3})(\d)/, "$1.$2")
        .replace(/(\d{3})(\d{1,2})$/, "$1-$2")
    }
    return value
}

function formatPhone(value: string) {
    value = value.replace(/\D/g, "")
    if (value.length <= 11) {
        return value
        .replace(/(\d{2})(\d)/, "($1) $2")
        .replace(/(\d{5})(\d)/, "$1-$2")
    }
    return value
}

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

export default function RegisterPage() {
    const router = useRouter()
    const [form, setForm] = useState({
        fullName: "",
        email: "",
        password: "",
        phone: "",
        cpf: "",
        pin: "",
        rg: "",
        birthDate: null as unknown as Date,
        balance: 0,
        createdAt: new Date(),
    })
    const [error, setError] = useState("")
    const [loading, setLoading] = useState(false)

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        setError("")
        setLoading(true)

        if (!isValidPassword(form.password)) {
            setError("A senha deve ter no mínimo 8 caracteres, incluindo maiúsculas, minúsculas, número e símbolo")
            setLoading(false)
            return
        }

        if (!isAdult(new Date(form.birthDate))) {
            setError("Você deve ter pelo menos 18 anos para criar uma conta")
            setLoading(false)
            return
        }

        if (!isValidEmail(form.email)) {
            setError("Email inválido")
            setLoading(false)
            return
        }

        if (!isValidCPF(form.cpf)) {
            setError("CPF inválido")
            setLoading(false)
            return
        }

        if (form.pin.length !== 4) {
            setError("O PIN deve ter exatamente 4 dígitos")
            setLoading(false)
            return
        }

        try {
            const res = await fetch("/api/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(form),
            })

            const data = await res.json()

            if (!res.ok) setError(data.error || "Erro ao registrar")
            else router.push("/dashboard")
        } catch {
            setError("Erro de rede")
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="flex justify-center items-center h-full">
            <form onSubmit={handleSubmit} className="bg-gray-800 p-8 rounded-2xl w-full max-w-lg space-y-4 shadow-lg">
                <h1 className="text-2xl font-bold text-blue-400 mb-4">Registrar</h1>
                {error && <p className="text-red-500">{error}</p>}
                <input type="name" placeholder="Nome Completo" className="w-full p-3 rounded bg-gray-700" 
                value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
                <input type="email" placeholder="Email" className="w-full p-3 rounded bg-gray-700" 
                value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                <input type="password" placeholder="Senha" className="w-full p-3 rounded bg-gray-700" 
                value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
                <div className="flex flex-row gap-4">
                    <input type="text" placeholder="Telefone" className="w-full p-3 rounded bg-gray-700" 
                    value={form.phone} onChange={(e) => setForm({ ...form, phone: formatPhone(e.target.value).slice(0, 17) })} />
                    <input type="text" placeholder="CPF" className="w-full p-3 rounded bg-gray-700" 
                    value={form.cpf} onChange={(e) => { const digits = e.target.value.replace(/\D/g, "").slice(0, 11); setForm({ ...form, cpf: formatCPF(digits) }) }}/>
                </div>
                <div className="flex flex-row gap-4">
                    <input type="text" placeholder="PIN" className="w-full p-3 rounded bg-gray-700" 
                    value={form.pin} onChange={(e) => setForm({ ...form, pin: e.target.value.replace(/\D/g, "").slice(0, 4) })} />
                    <input type="date" placeholder="" className="w-full p-3 rounded bg-gray-700" 
                    value={form.birthDate ? new Date(form.birthDate).toISOString().split("T")[0] : ""} onChange={(e) => setForm({ ...form, birthDate: new Date(e.target.value) })}/>
                </div>
                <input type="text" placeholder="RG (opcional)" className="w-full p-3 rounded bg-gray-700" 
                value={form.rg} onChange={(e) => setForm({ ...form, rg: e.target.value.replace(/\D/g, "") })} />
                <button type="submit" disabled={loading} className="w-full bg-blue-600 py-3 rounded hover:bg-blue-500">
                    {loading ? "Registrando..." : "Registrar"}
                </button>
                <button type="button" onClick={() => router.push("/login")} className="py-1.5 px-3 rounded bg-gray-600 hover:bg-gray-500">
                    Já tem conta?
                </button>
                <p className="text-sm">Ao continuar, você autoriza a consulta e o registro dos seus dados no nosso sistema, e também está de acordo com a <a href="/terms" className="text-blue-400">política de privacidade</a> do nosso banco.</p>
            </form>
        </div>
    )
}