"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { decimalFromTypedDigits, formatBRL, isPositiveDecimal } from "@/_lib/money"
import type { StatementItem } from "@/_lib/types"

type Step = "form" | "review" | "done"

type Receipt = {
    id: string
    amount: string
    toAccountId: string
    description: string | null
    createdAt: string
}

type ApiError = { error?: string; code?: string }

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const TYPE_LABEL: Record<StatementItem["type"], string> = {
    DEPOSIT: "Depósito",
    TRANSFER: "Transferência",
}

export default function TransferPage() {
    const [account, setAccount] = useState<{ accountId: string; balance: string } | null>(null)
    const [statement, setStatement] = useState<StatementItem[]>([])
    const [cursor, setCursor] = useState<number | null>(null)

    const [step, setStep] = useState<Step>("form")
    const [toAccountId, setToAccountId] = useState("")
    const [amount, setAmount] = useState("")
    const [description, setDescription] = useState("")
    const [pin, setPin] = useState("")
    const [error, setError] = useState("")
    const [sending, setSending] = useState(false)
    const [receipt, setReceipt] = useState<Receipt | null>(null)
    const [copied, setCopied] = useState(false)

    /**
     * Uma Idempotency-Key por INTENÇÃO de transferir: é criada ao revisar e reaproveitada
     * em qualquer nova tentativa (timeout, queda de rede, duplo clique). Assim o core
     * nunca executa a mesma transferência duas vezes. Só muda se o usuário editar os dados.
     */
    const idempotencyKey = useRef<string | null>(null)

    const loadAccount = useCallback(async () => {
        const res = await fetch("/api/user/balance")
        if (res.ok) setAccount(await res.json())
    }, [])

    const loadStatement = useCallback(async (from: number | null) => {
        const query = from ? `?cursor=${from}` : ""
        const res = await fetch(`/api/user/transactions${query}`)
        if (!res.ok) return
        const data: { transactions: StatementItem[]; nextCursor: number | null } = await res.json()
        setStatement((prev) => (from ? [...prev, ...data.transactions] : data.transactions))
        setCursor(data.nextCursor)
    }, [])

    useEffect(() => {
        loadAccount()
        loadStatement(null)
    }, [loadAccount, loadStatement])

    function review(e: React.FormEvent) {
        e.preventDefault()
        setError("")
        const to = toAccountId.trim()
        if (!UUID_RE.test(to)) return setError("Informe um número de conta válido")
        if (account && to.toLowerCase() === account.accountId.toLowerCase()) {
            return setError("Não é possível transferir para a própria conta")
        }
        if (!isPositiveDecimal(amount)) return setError("Informe um valor maior que zero")
        idempotencyKey.current ??= crypto.randomUUID()
        setStep("review")
    }

    function edit() {
        // Dados alterados = nova intenção = nova chave.
        idempotencyKey.current = null
        setPin("")
        setError("")
        setStep("form")
    }

    async function confirm(e: React.FormEvent) {
        e.preventDefault()
        if (sending) return
        if (!/^\d{6}$/.test(pin)) return setError("O PIN tem 6 dígitos")
        setSending(true)
        setError("")
        try {
            const res = await fetch("/api/user/transactions", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Idempotency-Key": idempotencyKey.current ?? crypto.randomUUID(),
                },
                body: JSON.stringify({
                    toAccountId: toAccountId.trim(),
                    amount,
                    description: description.trim() || undefined,
                    pin,
                }),
            })
            const data = await res.json()
            if (!res.ok) {
                const err = data as ApiError
                setPin("")
                if (err.code === "invalid_pin" || err.code === "pin_locked") {
                    setError(err.error ?? "PIN inválido")
                } else {
                    setError(err.error ?? "Não foi possível concluir a transferência")
                    // Falha temporária (servidor, conflito, limite): fica na revisão e reusa a chave.
                    const retryable = res.status >= 500 || res.status === 409 || res.status === 429
                    if (!retryable) {
                        // Erro de negócio (saldo, conta inexistente...): volta para corrigir os dados.
                        idempotencyKey.current = null
                        setStep("form")
                    }
                }
                return
            }
            setReceipt(data as Receipt)
            setStep("done")
            idempotencyKey.current = null
            loadAccount()
            loadStatement(null)
        } catch {
            // Sem resposta: a transferência PODE ter sido feita. Tentar de novo com a mesma
            // chave é seguro — o core devolve o resultado original em vez de transferir de novo.
            setError("Falha de conexão. Toque em confirmar para tentar novamente com segurança.")
        } finally {
            setSending(false)
        }
    }

    function newTransfer() {
        setToAccountId("")
        setAmount("")
        setDescription("")
        setPin("")
        setReceipt(null)
        setError("")
        setStep("form")
    }

    async function copyAccountId() {
        if (!account) return
        await navigator.clipboard.writeText(account.accountId)
        setCopied(true)
        setTimeout(() => setCopied(false), 2000)
    }

    return (
        <div className="p-6 md:p-10 w-full h-full overflow-y-auto flex flex-col lg:flex-row gap-8">
            <section className="w-full lg:max-w-md flex flex-col gap-6">
                <div className="rounded-2xl bg-gray-800 text-white p-6 flex flex-col gap-2">
                    <p className="text-sm text-gray-300">Saldo disponível</p>
                    <p className="text-3xl font-bold">{account ? formatBRL(account.balance) : "..."}</p>
                    {account && (
                        <div className="flex flex-col gap-1 mt-2">
                            <p className="text-sm text-gray-300">Sua conta (para receber)</p>
                            <div className="flex flex-row items-center gap-2">
                                <code className="text-xs break-all">{account.accountId}</code>
                                <button type="button" onClick={copyAccountId}
                                    className="text-xs px-2 py-1 rounded bg-gray-600 hover:bg-gray-500 shrink-0">
                                    {copied ? "Copiado" : "Copiar"}
                                </button>
                            </div>
                        </div>
                    )}
                </div>

                <div className="rounded-2xl bg-gray-800 text-white p-6">
                    {step === "form" && (
                        <form onSubmit={review} className="flex flex-col gap-4">
                            <h1 className="text-2xl font-bold text-blue-400">Transferir</h1>
                            {error && <p role="alert" className="text-red-400">{error}</p>}
                            <label className="flex flex-col gap-1">
                                <span className="text-sm text-gray-300">Conta de destino</span>
                                <input value={toAccountId} onChange={(e) => setToAccountId(e.target.value)}
                                    placeholder="00000000-0000-0000-0000-000000000000" autoComplete="off"
                                    className="p-3 rounded bg-gray-700 font-mono text-sm" />
                            </label>
                            <label className="flex flex-col gap-1">
                                <span className="text-sm text-gray-300">Valor</span>
                                <input inputMode="numeric" value={amount ? formatBRL(amount) : ""}
                                    onChange={(e) => setAmount(decimalFromTypedDigits(e.target.value))}
                                    placeholder="R$ 0,00" className="p-3 rounded bg-gray-700 text-xl" />
                            </label>
                            <label className="flex flex-col gap-1">
                                <span className="text-sm text-gray-300">Descrição (opcional)</span>
                                <input value={description} maxLength={140}
                                    onChange={(e) => setDescription(e.target.value)}
                                    className="p-3 rounded bg-gray-700" />
                            </label>
                            <button type="submit" className="w-full bg-blue-600 py-3 rounded hover:bg-blue-500">
                                Revisar
                            </button>
                        </form>
                    )}

                    {step === "review" && (
                        <form onSubmit={confirm} className="flex flex-col gap-4">
                            <h1 className="text-2xl font-bold text-blue-400">Confirme a transferência</h1>
                            <dl className="flex flex-col gap-2 text-sm">
                                <div>
                                    <dt className="text-gray-300">Valor</dt>
                                    <dd className="text-2xl font-bold">{formatBRL(amount)}</dd>
                                </div>
                                <div>
                                    <dt className="text-gray-300">Para a conta</dt>
                                    <dd className="font-mono text-xs break-all">{toAccountId.trim()}</dd>
                                </div>
                                {description.trim() && (
                                    <div>
                                        <dt className="text-gray-300">Descrição</dt>
                                        <dd>{description.trim()}</dd>
                                    </div>
                                )}
                            </dl>
                            {error && <p role="alert" className="text-red-400">{error}</p>}
                            <label className="flex flex-col gap-1">
                                <span className="text-sm text-gray-300">PIN de 6 dígitos</span>
                                <input type="password" inputMode="numeric" autoComplete="off" autoFocus value={pin}
                                    onChange={(e) => setPin(e.target.value.replace(/\D/g, "").slice(0, 6))}
                                    className="p-3 rounded bg-gray-700 tracking-[0.5em] text-xl" />
                            </label>
                            <button type="submit" disabled={sending}
                                className="w-full bg-blue-600 py-3 rounded hover:bg-blue-500 disabled:opacity-60">
                                {sending ? "Enviando..." : "Confirmar"}
                            </button>
                            <button type="button" onClick={edit} disabled={sending}
                                className="py-1.5 px-3 rounded bg-gray-600 hover:bg-gray-500">
                                Voltar e editar
                            </button>
                        </form>
                    )}

                    {step === "done" && receipt && (
                        <div className="flex flex-col gap-4">
                            <h1 className="text-2xl font-bold text-green-400">Transferência concluída</h1>
                            <dl className="flex flex-col gap-2 text-sm">
                                <div>
                                    <dt className="text-gray-300">Valor</dt>
                                    <dd className="text-2xl font-bold">{formatBRL(receipt.amount)}</dd>
                                </div>
                                <div>
                                    <dt className="text-gray-300">Para a conta</dt>
                                    <dd className="font-mono text-xs break-all">{receipt.toAccountId}</dd>
                                </div>
                                <div>
                                    <dt className="text-gray-300">Data</dt>
                                    <dd>{new Date(receipt.createdAt).toLocaleString("pt-BR")}</dd>
                                </div>
                                <div>
                                    <dt className="text-gray-300">Autenticação</dt>
                                    <dd className="font-mono text-xs break-all">{receipt.id}</dd>
                                </div>
                            </dl>
                            <button type="button" onClick={newTransfer}
                                className="w-full bg-blue-600 py-3 rounded hover:bg-blue-500">
                                Nova transferência
                            </button>
                        </div>
                    )}
                </div>
            </section>

            <section className="w-full flex flex-col gap-4">
                <h2 className="text-xl font-semibold">Extrato</h2>
                {statement.length === 0 ? (
                    <p className="text-gray-500">Nenhuma movimentação ainda.</p>
                ) : (
                    <ul className="flex flex-col divide-y divide-gray-200 rounded-2xl border border-gray-200">
                        {statement.map((item) => (
                            <li key={item.id} className="flex flex-row justify-between items-center p-4 gap-4">
                                <div className="flex flex-col">
                                    <span className="font-medium">{item.description || TYPE_LABEL[item.type]}</span>
                                    <span className="text-xs text-gray-500">
                                        {TYPE_LABEL[item.type]} · {new Date(item.createdAt).toLocaleString("pt-BR")}
                                    </span>
                                </div>
                                <div className="flex flex-col items-end">
                                    <span className={`font-semibold ${item.amount.startsWith("-") ? "text-red-600" : "text-green-600"}`}>
                                        {formatBRL(item.amount)}
                                    </span>
                                    <span className="text-xs text-gray-500">Saldo {formatBRL(item.balanceAfter)}</span>
                                </div>
                            </li>
                        ))}
                    </ul>
                )}
                {cursor && (
                    <button type="button" onClick={() => loadStatement(cursor)}
                        className="self-center py-2 px-4 rounded bg-gray-200 hover:bg-gray-300">
                        Carregar mais
                    </button>
                )}
            </section>
        </div>
    )
}