"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { formatBRL } from "@/_lib/money"
import type { Transaction } from "@/_lib/types"

export default function Dashboard() {
    const [balance, setBalance] = useState<string>("0.00")
    const [transactions, setTransactions] = useState<Transaction[]>([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        async function fetchData() {
            try {
                const [balanceRes, transactionsRes] = await Promise.all([
                    fetch("/api/user/balance"),
                    fetch("/api/user/transactions"),
                ])

                const balanceData = await balanceRes.json()
                const transactionsData = await transactionsRes.json()

                if (typeof balanceData.balance === "string") setBalance(balanceData.balance)
                setTransactions(Array.isArray(transactionsData.transactions) ? transactionsData.transactions : [])
            } catch (err) {
                console.error("Erro ao buscar dados:", err)
            } finally {
                setLoading(false)
            }
        }

        fetchData()
    }, [])

    // O extrato vem do mais recente para o mais antigo.
    const last = transactions[0]

    return (
        <div className="p-10 flex flex-row justify-between items-start h-full w-full gap-6">
            <div className="">
                <p className="text-xl font-semibold leading-tight">Saldo disponível</p>
                <p className="text-3xl font-bold">{loading ? "..." : formatBRL(balance)}</p>
                <Link href="/transfer" className="inline-block mt-3 px-4 py-2 rounded bg-blue-600 text-white hover:bg-blue-500">
                    Transferir
                </Link>
            </div>
            <div className="">
                <p className="text-xl font-semibold leading-tight">Transações recentes</p>
                <p className="text-3xl font-bold">{transactions.length} transações</p>
            </div>
            <div className="">
                <p className="text-xl font-semibold leading-tight">Sua última atividade</p>
                <p className="text-3xl font-bold">
                    {last ? `${last.description ?? last.type} (${formatBRL(last.amount)})` : "Nenhuma atividade"}
                </p>
            </div>
        </div>
    )
}
