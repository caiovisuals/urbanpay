"use client"

import { useState } from "react"

export default function Assistance() {
    const [form, setForm] = useState({ name: "", email: "", message: "" })
    const [message, setMessage] = useState("")
    const [loading, setLoading] = useState(false)

    const faqs = [
        { q: "Como posso resetar minha senha?", a: "Clique em 'Esqueci minha senha' na página de login e siga os passos." },
        { q: "Como alterar meus dados cadastrais?", a: "Acesse Configurações > Perfil e atualize seus dados." },
        { q: "Como entrar em contato com o suporte?", a: "Você pode enviar uma mensagem pelo formulário ou usar nosso chat online." },
    ]

    const [openFaqIndex, setOpenFaqIndex] = useState<number | null>(null)

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        setLoading(true)
        try {
            // Aqui você chamaria sua API de envio
            await new Promise((r) => setTimeout(r, 1000))
            setMessage("Mensagem enviada com sucesso!")
            setForm({ name: "", email: "", message: "" })
        } catch (err) {
            setMessage("Erro ao enviar a mensagem.")
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="p-10 flex flex-col size-full">
            <h1 className="text-2xl font-semibold mb-4">Assistência ao Usuário</h1>

            <form onSubmit={handleSubmit} className="mb-8 flex flex-col gap-4">
                <input
                    type="text"
                    placeholder="Nome"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    className="border p-2 rounded w-full"
                    required
                />
                <input
                    type="email"
                    placeholder="Email"
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    className="border p-2 rounded w-full"
                    required
                />
                <textarea
                    placeholder="Mensagem"
                    value={form.message}
                    onChange={(e) => setForm({ ...form, message: e.target.value })}
                    className="border p-2 rounded w-full h-32"
                    required
                />
                <button
                    type="submit"
                    disabled={loading}
                    className="bg-blue-500 text-white p-2 rounded hover:bg-blue-600 disabled:opacity-50"
                >
                    {loading ? "Enviando..." : "Enviar Mensagem"}
                </button>
                {message && <span className="text-green-500">{message}</span>}
            </form>

            {/* FAQ */}
            <div className="mb-8">
                <h2 className="text-2xl font-semibold mb-4">Perguntas Frequentes</h2>
                <div className="flex flex-col gap-2">
                    {faqs.map((faq, i) => (
                        <div key={i} className="border rounded p-3 cursor-pointer" onClick={() => setOpenFaqIndex(openFaqIndex === i ? null : i)}>
                            <div className="font-medium">{faq.q}</div>
                            {openFaqIndex === i && <div className="mt-2 text-gray-700">{faq.a}</div>}
                        </div>
                    ))}
                </div>
            </div>

            {/* Contato direto */}
            <div>
                <h2 className="text-2xl font-semibold mb-4">Contato Direto</h2>
                <div>
                    <p>Email: suporte@exemplo.com</p>
                    <p>Telefone: +55 71 99999-9999</p>
                </div>
            </div>
        </div>
    )
}
