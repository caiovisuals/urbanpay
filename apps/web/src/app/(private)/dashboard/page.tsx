"use client"

import { useEffect, useState } from "react"
import { Transaction } from "@/_lib/db"

export default function Dashboard() {
    const [balance, setBalance] = useState<number>(0)
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

            setBalance(typeof balanceData.balance === "number" ? balanceData.balance : 0)
            setTransactions(Array.isArray(transactionsData.transactions) ? transactionsData.transactions : [])
        } catch (err) {
            console.error("Erro ao buscar dados:", err)
        } finally {
            setLoading(false)
        }
        }

        fetchData()
    }, [])

    const formatCurrency = (value: number | null) => {
        if (value === null) return "0,00"
        return value.toFixed(2).replace(".", ",")
    }

    return (
        <div className="p-10 flex flex-row justify-between items-start h-full w-full gap-6">
            <div className="">
                <p className="text-xl font-semibold leading-tight">Saldo disponível</p>
                <p className="text-3xl font-bold">R$ {formatCurrency(balance)}</p>
            </div>
            <div className="">
                <p className="text-xl font-semibold leading-tight">Transações completas</p>
                <p className="text-3xl font-bold">{transactions.length} transações</p>
            </div>
            <div className="">
                <p className="text-xl font-semibold leading-tight">Sua última atividade</p>
                <p className="text-3xl font-bold">{transactions[transactions.length - 1]?.description || "Nenhuma atividade"}</p>
            </div>
        </div>
    )
}