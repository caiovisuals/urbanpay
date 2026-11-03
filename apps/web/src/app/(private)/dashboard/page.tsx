"use client"

import { useEffect, useState } from "react"
import type { Transaction } from "@/_lib/types"

/** Formata a string decimal do core ("1234.50") sem passar por aritmética de ponto flutuante. */
function formatBRL(value: string): string {
    const negative = value.startsWith("-")
    const [int, frac = "00"] = value.replace("-", "").split(".")
    const grouped = int.replace(/\B(?=(\d{3})+(?!\d))/g, ".")
    return `${negative ? "-" : ""}R$ ${grouped},${frac.padEnd(2, "0")}`
}

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
